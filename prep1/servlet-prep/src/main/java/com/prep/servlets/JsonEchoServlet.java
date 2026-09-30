package com.prep.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Day 1, Build #1: a servlet that accepts JSON and returns JSON,
 * with correct status codes.
 *
 * Deliberately hand-rolls the JSON handling (no Jackson/Gson) so the
 * mechanics of reading a request body and writing a response body are
 * visible. In a real Spring app this is what the framework does for you.
 *
 * Try it:
 *   curl -i -X POST http://localhost:8080/servlet-prep/echo \
 *        -H "Content-Type: application/json" \
 *        -d '{"message":"hello"}'
 *
 *   # wrong content type -> 415
 *   curl -i -X POST http://localhost:8080/servlet-prep/echo -d 'plain text'
 *
 *   # empty body -> 400
 *   curl -i -X POST http://localhost:8080/servlet-prep/echo \
 *        -H "Content-Type: application/json"
 */
@WebServlet(name = "jsonEchoServlet", urlPatterns = {"/echo"})
public class JsonEchoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Validate content type -> 415 Unsupported Media Type if wrong.
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            writeJson(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "{\"error\":\"Content-Type must be application/json\"}");
            return;
        }

        // 2. Read the raw request body.
        String body = readBody(req);

        // 3. Empty body -> 400 Bad Request.
        if (body.isBlank()) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"error\":\"Request body must not be empty\"}");
            return;
        }

        // 4. Echo it back wrapped in a small envelope -> 200 OK.
        //    (We treat the incoming body as an opaque JSON value.)
        String responseBody = "{\"echoed\":" + body + ",\"length\":" + body.length() + "}";
        writeJson(resp, HttpServletResponse.SC_OK, responseBody);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // GET isn't supported here -> 405 with an Allow header.
        resp.setHeader("Allow", "POST");
        writeJson(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "{\"error\":\"Use POST with a JSON body\"}");
    }

    /** Reads the entire request body as a UTF-8 string. */
    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }

    /** Writes a JSON string with the given status code and proper headers. */
    private void writeJson(HttpServletResponse resp, int status, String json)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            out.print(json);
        }
    }
}
