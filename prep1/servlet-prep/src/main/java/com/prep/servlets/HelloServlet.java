package com.prep.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Phase 0 / Phase 1 deliverable: a minimal servlet.
 *
 * Demonstrates:
 *  - @WebServlet annotation-based URL mapping (no web.xml needed)
 *  - the servlet lifecycle hooks (init / destroy) with logging
 *  - responding to GET vs POST differently
 *
 * Try it:
 *   GET  http://localhost:8080/servlet-prep/hello
 *   GET  http://localhost:8080/servlet-prep/hello?name=Priya
 *   POST http://localhost:8080/servlet-prep/hello
 */
@WebServlet(name = "helloServlet", urlPatterns = {"/hello"})
public class HelloServlet extends HttpServlet {

    /**
     * Called ONCE when the container loads the servlet.
     * Good place for one-time setup (never per-request work).
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("[HelloServlet] init() called - servlet instance created");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Query params come from the URL: /hello?name=Priya
        String name = req.getParameter("name");
        if (name == null || name.isBlank()) {
            name = "World";
        }

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        try (PrintWriter out = resp.getWriter()) {
            out.println("Hello, " + name + "! (served by GET)");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        try (PrintWriter out = resp.getWriter()) {
            out.println("Hello from a POST request!");
        }
    }

    /**
     * Called ONCE when the container shuts down or reloads the app.
     * Good place to release resources opened in init().
     */
    @Override
    public void destroy() {
        System.out.println("[HelloServlet] destroy() called - servlet being removed");
    }
}
