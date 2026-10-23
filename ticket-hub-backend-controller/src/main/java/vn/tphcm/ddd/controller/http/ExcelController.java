/*
 * @ (#) ExcelController.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.tphcm.ddd.application.service.excel.ExportExcelService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ExcelController {

    private final ExportExcelService exportExcelService;

    @PreAuthorize("hasRole('ADMIN') and authentication.name == 'admin'")
    @PostMapping("/export")
    public void exportData(HttpServletResponse response,
                           @RequestParam("nTable") String nTable) throws IOException {
        exportExcelService.exportData(response, nTable);
    }

    @PreAuthorize("hasRole('ADMIN') and authentication.name == 'admin'")
    @PostMapping("/exportWithThread")
    public void exportDataWithThread(HttpServletResponse response,@RequestParam("nTable") String nTable) throws IOException {
        exportExcelService.exportDataWithThread(response, nTable);
    }
}
