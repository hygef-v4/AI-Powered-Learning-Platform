package edu.aiplatform.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.Actor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Complete educator workflows ship with the backend, independent of the caller's local skills. */
@Component
public class McpAgentWorkflowTools implements IMcpToolProvider {
    private final Map<String,Map<String,Object>> workflows;

    /**
     * Initializes the workflow tool provider and pre-loads bundled workflow instruction skills.
     */
    public McpAgentWorkflowTools(ObjectMapper json) {
        workflows=Map.of("AUTHOR_ASSIGNMENT",load(json,"AUTHOR_ASSIGNMENT","author-assignment"),
                "GRADE_WITH_RUBRIC",load(json,"GRADE_WITH_RUBRIC","grade-with-rubric"));
    }

    /**
     * Declares the read_agent_workflow MCP tool definition and its allowed workflow options.
     */
    @Override public List<McpToolDefinition> tools() {
        return List.of(McpSchemas.read("read_agent_workflow",
                "Read complete server-managed instructions before authoring assignments or grading. No local skill installation required. Follow current domain tool schemas and return reviewUrl for human review.",
                "\"workflow\":{\"enum\":[\"AUTHOR_ASSIGNMENT\",\"GRADE_WITH_RUBRIC\"]}","workflow"));
    }

    /**
     * Handles tool execution to return the requested workflow instructions and fingerprint version.
     */
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        // Step 1: Verify that caller holds an active teaching role (TEACHER or SUBJECT_MANAGER)
        McpInput.teaching(actor);
        if(!"read_agent_workflow".equals(name))throw McpInput.invalid();

        // Step 2: Extract requested workflow name and lookup pre-loaded instructions
        var workflow=workflows.get(new McpInput(args).text("workflow",""));
        if(workflow==null)throw McpInput.invalid();

        // Step 3: Return workflow instruction content, hash version, and workflow identifier
        return new McpToolResult(false,workflow);
    }

    /**
     * Loads markdown workflow instructions from classpath resources and generates fingerprint version.
     */
    private static Map<String,Object> load(ObjectMapper json,String workflow,String directory) {
        try {
            String instructions=new ClassPathResource("agent-workflows/"+directory+"/SKILL.md")
                    .getContentAsString(StandardCharsets.UTF_8).replace("\r\n","\n");
            if(instructions.isBlank())throw new IllegalStateException("Empty packaged MCP workflow: "+workflow);
            return Map.of("workflow",workflow,"version",McpFingerprint.of(json,instructions),"instructions",instructions);
        } catch(IOException ex){throw new IllegalStateException("Missing packaged MCP workflow: "+workflow,ex);}
    }
}
