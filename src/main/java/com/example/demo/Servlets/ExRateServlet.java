package com.example.demo.Servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebServlet(name = "ExchangeRate", value = "/exchangeRate/*")
public class ExRateServlet extends EntityServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String pairCurrency, from, to;
        try {
            pairCurrency = request.getRequestURI().split("/")[2];
            if (pairCurrency.length() != 6) throw new Exception();
            from = pairCurrency.substring(0,3);
            to = pairCurrency.substring(3,6);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse(response, "Pair currency codes are missing or incorrect in the address");
            return;
        }

        Integer idPair = findPairIdByCodes(from, to);
        if (idPair == -1) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            errorResponse(response, "The exchange rate for the pair was not found");
        } else {
            String query = "SELECT * FROM ExchangeRates WHERE ID="+idPair;
            super.doGet(response, query);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getMethod().equals("PATCH")) {
            super.service(req, resp);
        } else {
            this.doPatch(req, resp);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) {
        String rate = request.getParameter("rate");
        if (rate == null || rate.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse(response, "Parameter «rate» is empty");
            return;
        }

        String pairCurrency, from, to;
        try {
            pairCurrency = request.getRequestURI().split("/")[2];
            if (pairCurrency.length() != 6) throw new Exception();
            from = pairCurrency.substring(0,3);
            to = pairCurrency.substring(3,6);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse(response, "Pair currency codes are missing or incorrect in the address");
            return;
        }
        Integer idPair = findPairIdByCodes(from, to);

        if (idPair == -1) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            errorResponse(response, "The currency pair is not in the database");
        } else {
            String query = "UPDATE ExchangeRates SET Rate = "+rate+" WHERE ID = "+idPair;
            try {
                PreparedStatement statement = connection.prepareStatement(query);
                statement.execute();
                statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            super.doGet(response, "SELECT * FROM ExchangeRates WHERE ID = "+idPair);
        }
    }

    private Integer findPairIdByCodes(String codeFrom, String codeTo) {
        Integer res = -1;
        try {
            String query = "SELECT * FROM ExchangeRates ex " +
                           "JOIN Currencies cur ON cur.ID = ex.BaseCurrencyId " +
                           "JOIN Currencies cur1 ON cur1.ID = ex.TargetCurrencyId " +
                           "WHERE cur.Code = \"" + codeFrom + "\" AND cur1.Code = \"" + codeTo + "\" ";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                res = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
            return res;
        } catch (Exception e) {
            return -1;
        }
    }
}
