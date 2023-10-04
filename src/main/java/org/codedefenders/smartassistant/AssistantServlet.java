package org.codedefenders.smartassistant;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.util.Paths;

import com.google.gson.Gson;

@WebServlet(Paths.SMART_ASSISTANT)
public class AssistantServlet extends HttpServlet {
    @Inject
    private AssistantService assistantService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //TODO: check authentication, assistant settings, game ID
        String question = request.getParameter("question");
        String answer = assistantService.sendQuestionWithNoContext(question);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("question", question);
        responseBody.put("answer", answer);
        String json = new Gson().toJson(responseBody);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
