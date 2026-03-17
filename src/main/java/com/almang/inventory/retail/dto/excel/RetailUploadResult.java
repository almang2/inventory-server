package com.almang.inventory.retail.dto.excel;

import java.util.List;

// 업로드 결과를 담는 내부 클래스
public record RetailUploadResult(
        int processedCount,  // 처리된 상품 수
        List<SkippedRow> skippedRows  // 스킵된 상품 목록 (코드 + 상품명)
) {}