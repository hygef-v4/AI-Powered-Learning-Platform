package edu.aiplatform.materials.integration;

import edu.aiplatform.files.contract.IArtifact;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class MaterialTextExtractor {
    public record Result(String text,int pages) { }
    private final IArtifact artifacts;
    private final Duration deadline;
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(0,2,30,TimeUnit.SECONDS,
            new SynchronousQueue<>(),r->{Thread t=new Thread(r,"material-extract");t.setDaemon(true);return t;});
    @org.springframework.beans.factory.annotation.Autowired
    public MaterialTextExtractor(IArtifact artifacts) {this(artifacts,Duration.ofSeconds(120));}
    MaterialTextExtractor(IArtifact artifacts,Duration deadline) {this.artifacts=artifacts;this.deadline=deadline;}
    public Result extract(String fileId) {
        AtomicReference<InputStream> activeStream=new AtomicReference<>();
        AtomicReference<Process> activeProcess=new AtomicReference<>();
        Future<Result> task=executor.submit(()->parse(fileId,activeStream,activeProcess));
        try {return task.get(deadline.toMillis(),TimeUnit.MILLISECONDS);}
        catch(InterruptedException e) {Thread.currentThread().interrupt();throw new IllegalStateException("Extraction interrupted");}
        catch(ExecutionException|TimeoutException e) {throw new IllegalStateException("Extraction failed");}
        finally {
            task.cancel(true);
            Process process=activeProcess.get();if(process!=null&&process.isAlive())process.destroyForcibly();
            InputStream stream=activeStream.get();if(stream!=null)try{stream.close();}catch(IOException ignored){ }
        }
    }
    public Result extract(String fileId, String mediaType) {
        if (!"text/plain".equals(mediaType)) return extract(fileId);
        try (InputStream input = artifacts.open(fileId)) {
            byte[] bytes = input.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw new IllegalArgumentException("Source is too large");
            return new Result(decodeText(bytes), 1);
        } catch (IOException e) {
            throw new IllegalStateException("Text source could not be read", e);
        }
    }
    static String decodeText(byte[] bytes) {
        try {
            String value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return value.startsWith("\uFEFF") ? value.substring(1) : value;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Text source must be UTF-8", e);
        }
    }
    private Result parse(String fileId,AtomicReference<InputStream> activeStream,AtomicReference<Process> activeProcess) throws Exception {
        Path directory=Files.createTempDirectory("material-extract-");Process process=null;
        try {
            Path input=directory.resolve("input"),output=directory.resolve("output");
            try(InputStream in=artifacts.open(fileId);OutputStream out=Files.newOutputStream(input)) {
                activeStream.set(in);byte[] buffer=new byte[8192];long total=0;int read;
                while((read=in.read(buffer))!=-1) {
                    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                    total+=read;if(total>50L*1024*1024)throw new IOException("Input too large");out.write(buffer,0,read);
                }
            } finally {activeStream.set(null);}
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            String classPath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
            List<String> command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                    "-Xmx256m","-XX:MaxMetaspaceSize=128m","-Djava.awt.headless=true"));
            boolean bootJar=!classPath.contains(File.pathSeparator)&&classPath.endsWith(".jar");
            if(bootJar)command.add("-Dloader.main="+MaterialParserMain.class.getName());
            command.addAll(List.of("-cp",classPath,bootJar?"org.springframework.boot.loader.launch.PropertiesLauncher":MaterialParserMain.class.getName(),
                    input.toString(),output.toString()));
            process=new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();activeProcess.set(process);
            if(!process.waitFor(120,TimeUnit.SECONDS)||process.exitValue()!=0)throw new IOException("Parser failed");
            if(Files.size(output)>4_000_008)throw new IOException("Parser output too large");
            try(DataInputStream in=new DataInputStream(Files.newInputStream(output))) {
                int pages=in.readInt(),length=in.readInt();
                if(length<0||length>4_000_000||pages<1)throw new IOException("Invalid parser result");
                byte[] bytes=in.readNBytes(length);if(bytes.length!=length)throw new IOException("Incomplete result");
                return new Result(new String(bytes,StandardCharsets.UTF_8),pages);
            }
        } finally {
            if(process!=null&&process.isAlive()) {process.destroyForcibly();process.waitFor(5,TimeUnit.SECONDS);}
            Files.deleteIfExists(directory.resolve("input"));Files.deleteIfExists(directory.resolve("output"));Files.deleteIfExists(directory);
        }
    }
    @PreDestroy public void close() {executor.shutdownNow();}
}
