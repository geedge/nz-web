package com.nis.modules.terminal.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.WebSocketSession;

import com.nis.common.utils.R;
import com.nis.modules.terminal.config.TerminalClient;
import com.nis.modules.terminal.config.TerminalContext;
import com.nis.modules.terminal.config.TerminalOneProcessor;
import com.nis.modules.terminal.config.TerminalSession;
import com.nis.modules.terminal.entity.Account;

@Controller
@RequestMapping("/terminal")
public class TerminalHandlerController {
	
	private Logger logger = LoggerFactory.getLogger(TerminalHandlerController.class);
	
	@Autowired
    private TerminalOneProcessor terminalOneProcessor;
	
	@Autowired
    private TerminalContext terminalContext;
	
	@RequestMapping(value = "resize", method = RequestMethod.POST)
    @ResponseBody
    public R resize(String uuid, Integer cols, Integer rows, Integer width, Integer height) throws Exception {
        return terminalOneProcessor.resize(uuid, cols, rows, width, height);
    }
	
    @SuppressWarnings("unused")
	private void closeWithTokens(String ...tokens) throws IOException {
        for (String token:tokens){
            Account account = terminalContext.remove(token);
            if(account!=null){
                String reKey = account.getId() + "_" + token;
                terminalContext.remove(reKey);
            }
            TerminalClient terminalClient = TerminalSession.get(token);
            if(terminalClient!=null){
                terminalClient.write("client will be closed..");
                WebSocketSession webSocketSession = terminalClient.getWebSocketSession();
                TerminalSession.remove(webSocketSession);
                terminalClient.disconnect();
            }

        }
    }
}
