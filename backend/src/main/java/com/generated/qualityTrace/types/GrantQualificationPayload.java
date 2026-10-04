package com.generated.qualityTrace.types;

/**
 * 授予检验员资质请求体。
 */
public record GrantQualificationPayload(Long inspectorId,
                                        String inspectionType,
                                        String qualifiedFrom,
                                        String qualifiedUntil) {
}
