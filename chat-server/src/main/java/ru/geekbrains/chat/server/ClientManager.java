package ru.geekbrains.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    private final Socket socket;

    private BufferedReader bufferedReader;

    private BufferedWriter bufferedWriter;

    private String name;

    public final static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        this.socket = socket;

        try {

            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + "подклчился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        }
        catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String massageFromClient;
        while (socket.isConnected()) {
            try {
                String messageFromClient = bufferedReader.readLine();
                if (messageFromClient.startsWith("@")) {
                    handlePrivateMessage(messageFromClient);
                } else {
                    broadcastMessage(messageFromClient); // Если сообщение не личное, отправляем его всем
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void broadcastMessage(String message) {
        for (ClientManager client : clients) {
            try {
                if (!client.name.equals(name)) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();

                }
            } catch (IOException e) {
                closeEverything (socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
// Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
// Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
// Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient(){
        clients.remove(this);
        System.out.println(name +" покинул чат.");
    }

    private void handlePrivateMessage(String message) {
        int spaceIndex = message.indexOf(" ");
        if (spaceIndex > 0) {
            String recipientName = message.substring(1, spaceIndex).trim(); // Получатель
            String privateMessage = message.substring(spaceIndex + 1).trim(); // Сообщение

            if (!privateMessage.isEmpty()) {
                sendPrivateMessage(recipientName, "Личное сообщение от " + name + ": " + privateMessage);
            } else {
                try {
                    bufferedWriter.write("Ошибка: Личное сообщение не может быть пустым.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        } else {
            try {
                bufferedWriter.write("Ошибка: Неверный формат личного сообщения.");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void sendPrivateMessage(String recipientName, String message) {
        boolean userFound = false;
        for (ClientManager client : clients) {
            if (client.name.equals(recipientName)) {
                userFound = true;
                try {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    return; // Выход после успешной отправки
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
        // Обработка случая, когда пользователь не найден
        if (!userFound) {
            try {
                bufferedWriter.write("Ошибка: Пользователь " + recipientName + " не найден.");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }



}