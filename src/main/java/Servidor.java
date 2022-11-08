package main.java;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

public class Servidor {

    enum State {
        CHAT,
        QUIZ,
        BREAK
    }

    public static int PORT = 4000;
    private ServerSocket serverSocket;
    private final List<ClienteSocket> clientes = new LinkedList<>();
    private final List<ClienteSocket> clientesRespondidos = new LinkedList<>();

    private State serverState = State.CHAT;

    private int contadorDePerguntas = 0;
    private int perguntasPorQuiz = 3;

    // Inicialização do server socket
    public void start() throws IOException
    {
        System.out.println("Digite a porta do servidor: ");
        Scanner scanner = new Scanner(System.in);
        PORT = scanner.nextInt();

        serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta: " + PORT);
        clientConnectionLoop();
    }

    // Fica em loop infinito verificando se algum cliente esta tentando se conectar ao servidor
    private void clientConnectionLoop() throws IOException
    {
        while(true)
        {
            // Metodo Accept fica aguardando conexao do cliente
            // Caso alguem se conecte, cria um clientSocket para representar essa conexao
            ClienteSocket clientSocket = new ClienteSocket(serverSocket.accept());
            sendMsg(clientSocket, "Digite seu username");
            clientSocket.setUsername();

            //Inicia nova Thread para poder gerenciar chat / quiz podendo simultaneamente receber novas conexoes
            new Thread(() -> clientMessageLoop(clientSocket)).start();
        }
    }

    // Loop que e iniciado quando um cliente se conecta
    // Responsavel por receber a mensagem do cliente e trata-la de acordo com o estado no qual o servido se encontra (chat, quiz, ou break)
    private void clientMessageLoop(ClienteSocket clientSocket)
    {
        // Inicializacao do cliente no servidor
        String msg;
        clientSocket.setUsername();

        String username = clientSocket.getUsername();
        clientes.add(clientSocket);

        msg = """
                --------------------- INSTRUCOES ---------------------------
                Digite "sair" para sair do aplicativo
                Digite "iniciar quiz" para iniciar o quiz com 3 perguntas
                Digite "iniciar quiz X" para iniciar o quiz com X perguntas
                ------------------------------------------------------------
                """;
        msg += String.format("Total de perguntas carregadas: %d\n", Reader.getAmountOfQuestions());
        sendMsg(clientSocket, msg);

        msg = username + " entrou no chat!";
        sendMsgToAll(msg);


        // Loop que verifica se recebeu alguma mensagem e a trata de acordo com o estado do servidor
        while(true) {
            if((msg = clientSocket.getMessage()) != null){

                //Recebimento de mensagem "sair" - qualquer estado
                if(msg.equalsIgnoreCase("sair")){
                    sendMsgToAll(clientSocket, String.format("%s saiu do chat!", username));
                    System.out.printf("Cliente %s desconectou\n", clientSocket.getRemoteSocketAddress());
                    clientes.remove(clientSocket);
                    break;
                }

                // Recebimento de mensagem no estado CHAT
                // Encaminha mensagem para todos os clientes no chat e possivelmente inicia o quiz
                if(getState() == State.CHAT) {
                    System.out.println("Recebeu uma mensagem no estado Chat");
                    sendMsgToAll(clientSocket, username + ": " + msg);
                    msg.toLowerCase();

                    //Caso a mensagem contenha "iniciar quiz", executa procedimento para inicio do quiz
                    if(msg.contains("iniciar quiz"))	{
                        //Verifica se quem iniciou quiz informou quantidade de perguntas
                        if(!msg.replaceAll("\\D+","").equals("")) perguntasPorQuiz = Integer.parseInt(msg.replaceAll("\\D+",""));
                        else perguntasPorQuiz = 3; //Se nao informou, fica com padrao 3
                        Reader.generateRandomQuestions(perguntasPorQuiz);
                        startQuiz();
                    }
                }

                // Recebimento de mensagem no estado QUIZ
                // (Verifica se acertou a resposta e verifica se todos player ja finalizaram)
                else if(getState() == State.QUIZ) {
                    System.out.println("Recebeu uma mensagem no estado Quiz");
                    verifyAnswer(clientSocket, msg);
                    if(verifyEnd())	startChat();
                }

                // Recebimento de mensagem no estado Break
                // (Encaminha mensagem para todos os clientes no break e possivelmente continua o quiz)
                else if(getState() == State.BREAK) {
                    System.out.println("Recebeu uma mensagem no estado Break");
                    sendMsgToAll(clientSocket, username + ": " + msg);

                    // Caso a mensagem for igual a "Continuar", da continuidade no quiz
                    if(msg.equalsIgnoreCase("Continuar")) startQuiz();
                }
            }
        }
    }

    // Funcao responsavel por iniciar um break
    private void startBreak(){
        setState(State.BREAK);
        String temp = """
                -----------------------------------------------------------------------------
                Indo para o Intervalo! Aqui você pode conversar com os outros participantes.
                Digite 'continuar' quando quiser ir para a proxima pergunta!
                -----------------------------------------------------------------------------
                """;
        sendMsgToAll(temp);
    }

    // Funcao responsavel por iniciar uma pergunta no quiz
    private void startQuiz(){
        contadorDePerguntas += 1;
        setState(State.QUIZ);
        sendMsgToAll(getQuestion(contadorDePerguntas - 1) + "\nDigite a alternativa correta: ");
    }

    // Funcao responsavel por iniciar um chat
    private void startChat(){
        setState(State.CHAT);
    }

    //Verifica a resposta de um cliente
    private void verifyAnswer (ClienteSocket clientSocket, String msg) {
        //Verifica se o player ja respondeu antes
        if(clientesRespondidos.contains(clientSocket))
        {
            sendMsg(clientSocket, "Voce ja respondeu!");
        } else {
            String temp = String.format("Voce escolhe a alternativa (%s). Aguardando todos responderem", msg);
            sendMsg(clientSocket, temp);

            //Verifica se acertou resposta
            clientesRespondidos.add(clientSocket);
            if(msg.equals(getAnswer(contadorDePerguntas - 1))) clientSocket.acertou();
        }

        //Verifica se todos clientes ja responderam. Caso sim, inicia um break
        if(haveAllAnswers()){
            sendMsgToAll(String.format("A resposta correta era: \"%s\"", getAnswer(contadorDePerguntas - 1)));
            if(contadorDePerguntas < perguntasPorQuiz) {
                clientesRespondidos.clear();
                startBreak();
            }
        }
    }

    // Verifica se todas perguntas ja foram respondidas (retorna true caso sim e false caso contrario)
    // Caso sim, imprime o resultado
    private boolean verifyEnd(){
        if(contadorDePerguntas >= perguntasPorQuiz && haveAllAnswers()){
            String ranking = "------- RESULTADOS --------\n";
            Collections.sort(clientes);
            for (int i = 1; i <= clientes.size(); i++){
                ranking += String.format("%d° - %s: %d/%d\n", i, clientes.get(i - 1).getUsername(), clientes.get(i - 1).getAcertos(), perguntasPorQuiz);
                clientes.get(i - 1).resetAcertos();
            }
            ranking += "\nQuiz finalizado, voltando ao chat";
            clientesRespondidos.clear();
            sendMsgToAll(ranking);
            contadorDePerguntas = 0;
            return true;
        }
        return false;
    }

    // Envia a String "msg" para todos clientes, exceto sender
    private void sendMsgToAll(ClienteSocket sender, String msg)
    {
        Iterator<ClienteSocket> iterator = clientes.iterator();
        while(iterator.hasNext())
        {
            ClienteSocket clientSocket = iterator.next();
            if(!sender.equals(clientSocket)) //se quem enviou nao é igual ao cliente atual do loop (para nao enviar a mensagem para quem mandou)
            {
                if(!clientSocket.sendMsg(msg))
                {
                    iterator.remove(); //quando a conexao de um cliente cair o servidor vai remover esse cliente
                }
            }
        }
    }

    // Envia a String "msg" somente para sender
    private void sendMsg(ClienteSocket sender, String msg)
    {
        sender.sendMsg(msg);
    }

    // Envia a String "msg" para todos clientes
    private void sendMsgToAll(String msg) //Servidor encaminha a mensagem para todos
    {
        Iterator<ClienteSocket> iterator = clientes.iterator();
        while(iterator.hasNext())
        {
            ClienteSocket clientSocket = iterator.next();
            if(!clientSocket.sendMsg(msg))
            {
                iterator.remove(); //quando a conexao de um cliente cair o servidor vai remover esse cliente
            }
        }
    }

    // Verifica se todos clientes ja responderam (retorna true caso sim e false caso contrario)
    public boolean haveAllAnswers(){
        for (ClienteSocket c : clientes)
            if(!clientesRespondidos.contains(c)) return false;

        return true;
    }

    public static void main(String[] args) {
        //Iniciar servidor
        try {
            Servidor server = new Servidor();
            server.start();

        } catch (IOException ex) {
            System.out.println("Erro ao iniciar o servidor: " + ex.getMessage());
        }

        System.out.println("Servidor finalizado");
    }

    // --- Getters e Setters --- //

    public State getState(){

        return this.serverState;
    }

    public void setState(State state){
        this.serverState = state;
    }

    public String getQuestion(int i){
        return Reader.questionsArray[i];
    }

    public String getAnswer(int i){
        return Reader.answersArray[i];
    }

}

