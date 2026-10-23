/*
 * @ (#) ExportExcelServiceImpl.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.excel.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.service.excel.ExportExcelService;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EXPORT-EXCEL")
public class ExportExcelServiceImpl implements ExportExcelService {
    private final OrderDeductionDomainService orderDeductionDomainService;
    private static final String XLS_CONTENT_TYPE = "applcation/vnd.ms-excel";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8";
    private static final String CSV_CONTENT_TYPE = "text/csv";

    @Override
    public void exportData(HttpServletResponse response, String nTable) throws IOException {
        List<Object[]> list = orderDeductionDomainService.findAll(nTable);

        String fileNameExport = "data-" + System.currentTimeMillis() + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileNameExport);

        ExcelWriterBuilder excelWriterBuilder = EasyExcelFactory.write(response.getOutputStream(), TicketOrder.class);
        excelWriterBuilder.registerWriteHandler(new WriteHandler() {
        }).excelType(ExcelTypeEnum.XLSX).sheet().doWrite(list);
    }

    @Override
    public void exportDataWithThread(HttpServletResponse response, String nTable) throws IOException {
        log.info("handleExportTask-> {}", Thread.currentThread().getName());
        long startTime = System.currentTimeMillis();
    }
}
