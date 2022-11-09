package main.java;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

public class ClienteSocket implements Comparable<ClienteSocket> {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    private int acertos;
    private String username;

    public ClienteSocket(Socket socket) throws IOException
    {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //recebe mensagem do cliente
        this.out = new PrintWriter(socket.getOutputStream(), true); //envia mensagens para o servidor
    }


    // Fecha Reader / Writters e o socket
    public void close()
    {
        try {
            in.close();
            out.close();
            socket.close();
        } catch(IOException e)
        {
            System.out.println("Erro ao fechar socket: " + e.getMessage());
        }
    }

    // Le username pela stdin
    public void setUsername(){
        try{
            this.username = in.readLine();
        } catch(IOException e){
            this.username = null;
        }
    }

    // Metodo que permite ordenacao da lista baseada na quantidade de acertos
    @Override
    public int compareTo(ClienteSocket o) {
        if(this.acertos < o.acertos) return 1;
        else if (this.acertos > o.acertos) return -1;
        return 0;
    }


    public void resetAcertos() {
        this.acertos = 0;
    }

    //Le mensagem pelo stdin
    public String getMessage()
    {
        try {
            return in.readLine();
        } catch(IOException e){
            return null;
        }
    }

    // Envia String (msg) atraves do PrintWritter
    public boolean sendMsg(String msg)
    {
        out.println(msg);
        return !out.checkError();
    }

    // -- Getters e Setters -- //
    public String getUsername()
    {
        return username;
    }

    public void acertou(){
        acertos++;
    }

    public int getAcertos(){
        return acertos;
    }

    public SocketAddress getRemoteSocketAddress()
    {
        return socket.getRemoteSocketAddress();
    }

}
