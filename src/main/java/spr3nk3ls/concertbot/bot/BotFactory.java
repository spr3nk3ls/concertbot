package spr3nk3ls.concertbot.bot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;

import static com.vsubhuman.telegram.TelegramFactory.sender;
import static java.lang.System.getenv;

public abstract class AbstractBot implements RequestStreamHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Init the util that will send messages for us
    private static final AbsSender SENDER = sender(getenv("bot_token"), getenv("bot_username"));

    protected AbstractBot() {
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        Update update;
        try {
            update = MAPPER.readValue(input, Update.class);
        } catch (Exception e) {
            System.err.println("Failed to parse update: " + e);
            throw new RuntimeException("Failed to parse update!", e);
        }
        System.out.println("Starting handling update " + update.getUpdateId());
        System.out.println("Starting handling update " + update);
        try {
            handleUpdate(update);
        } catch (Exception e) {
            System.err.println("Failed to handle update: " + e);
            throw new RuntimeException("Failed to handle update!", e);
        }
        System.out.println("Finished handling update " + update.getUpdateId());
    }

    private void handleUpdate(Update update) throws Exception {
        Message message = update.getMessage();
        if (message == null) {
            return;
        }
        String responseText;
        if(isGroupAdd(message)){
            responseText = handleGroupAdd(message);
        } else if(message.isGroupMessage() || message.isSuperGroupMessage()){
            responseText = handleGroupResponse(message);
        } else {
            responseText = handlePrivateResponse(message);
        }
        if(responseText != null) {
            sendMessage(message.getChatId(), responseText);
        }
    }

    protected void sendMessage(Long chatId, String responseText) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(chatId)
                .setText(responseText);
        System.out.println("Sending message: " + sendMessage);
        try {
            Message message = SENDER.execute(sendMessage);
            System.out.println("Message sent: " + message);
        } catch (Exception e) {
            System.err.println("Failed to send mesage: " + e);
            throw new RuntimeException("Failed to send message!", e);
        }
    }

    protected boolean isGroupAdd(Message message){
        if((message.isGroupMessage() || message.isSuperGroupMessage())
                && ((message.getGroupchatCreated() != null && message.getGroupchatCreated()) ||
                (message.getNewChatMembers() != null
                        && !message.getNewChatMembers().isEmpty()
                        && message.getNewChatMembers().stream().map(User::getUserName).anyMatch(x -> x.equals("blikbierbot"))))){
            return true;
        }
        return false;
    }

    protected abstract String handleGroupAdd(Message message);

    protected abstract String handlePrivateResponse(Message message);

    protected abstract String handleGroupResponse(Message message);

    protected AbsSender getSender(){
        return SENDER;
    }
}