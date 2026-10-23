/*
 * @ (#) ExportExcelService.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.excel;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface ExportExcelService {
    void exportData(HttpServletResponse response, String nTable) throws IOException;

    void exportDataWithThread(HttpServletResponse response, String nTable) throws IOException;
}
