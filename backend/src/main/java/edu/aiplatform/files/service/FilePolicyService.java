package edu.aiplatform.files.service;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.model.FileType;
import edu.aiplatform.files.model.PurposeLimits;
import edu.aiplatform.files.model.PurposePolicy;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/** F5 Giới hạn tải lên cho frontend; backend vẫn kiểm lại khi upload. */
@Service
public class FilePolicyService {

    private final PurposePolicy policy;

    public FilePolicyService(PurposePolicy policy) {
        this.policy = policy;
    }

    public record PurposePolicyView(ArtifactPurpose purpose, int maxSizeMb, List<String> allowedTypes,
                                    List<String> extensions, List<String> mediaTypes) {
    }

    public List<PurposePolicyView> policies() {
        return Arrays.stream(ArtifactPurpose.values()).map(purpose -> {
            PurposeLimits limits = policy.limits(purpose);
            List<FileType> types = limits.allowedTypes().stream().sorted().toList();
            return new PurposePolicyView(purpose, limits.maxSizeMb(), types.stream().map(Enum::name).toList(),
                    types.stream().flatMap(type -> type.extensions().stream()).toList(),
                    types.stream().map(FileType::mediaType).toList());
        }).toList();
    }
}
