package main.java;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Cliente implements Runnable{
    private ClienteSocket clientSocket;
    private final Scanner scanner;

    public Cliente()
    {
        scanner = new Scanner(System.in);
    }

    public void start() throws IOException
    {
        // Inicia conexao do Cliente ao servidor
        try {
            System.out.println("Digite o IP do servidor (IP Local: 127.0.0.1)");
            String server_address = scanner.nextLine();

            System.out.println("Digite a porta");
            int porta = scanner.nextInt();

            // Cria ClientSocket para gerenciar conexao cliente-serivdor
            clientSocket = new ClienteSocket(new Socket(server_address, porta));

            //Inicia Thread para ficar constantemente recebendo mensangens do servidor, enquanto simultaneamente ainda pode enviar mensgens no messageLoop
            new Thread(this).start();
            //Fica continuamente verificando se novas mensagens foram digitadas
            messageLoop();
        } finally {
            clientSocket.close();
        }
    }

    @Override
    public void run()
    {
        //Quando o servidor enviar uma mensagem (msg != null) imprime essa mensagem na tela
        String msg;
        while((msg = clientSocket.getMessage()) != null) System.out.println(msg);
    }

    // Fica em loop esperando uma nova linha do cliente. Quando recebe nova linha, envia para servidor
    private void messageLoop() {
        String msg;

        do{
            msg = scanner.nextLine(); // Espera pela proxima linha
            clientSocket.sendMsg(msg); // Envia para servidor

        } while(!msg.equalsIgnoreCase("sair")); // Verifica se mensagem foi "sair", caso sim, para execucao do programa


        System.out.println("Voce saiu do chat!");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            Cliente client = new Cliente();
            client.start();

        } catch (IOException ex){
            System.out.println("Erro ao iniciar cliente: " + ex.getMessage());
        }
    }

}
