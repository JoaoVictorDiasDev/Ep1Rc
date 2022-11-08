package main.java;

import javax.imageio.stream.FileImageInputStream;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

// Classe responsavel por ler conteudo do arquivo perguntas.txt
public class Reader {

    private static String currentAnswer; // Resposta para pergunta atual
    public static String[] questionsArray; // Array com todas perguntas de um quiz
    public static String[] answersArray; // Array com todas respostas de um quiz
    public static void main(String[] args) throws Exception {

    }

    // Retorna quantidade de perguntas no quiz
    public static int getAmountOfQuestions(){
        try {
            InputStream in = Reader.class.getClassLoader().getResourceAsStream("perguntas.txt");
            BufferedReader bf = new BufferedReader(new InputStreamReader(in));

            int res = Integer.parseInt(bf.readLine().replaceAll("\\D+",""));
            bf.close();
            return  res;

        } catch (IOException e){
            System.out.println("Problema ao carregar arquivo em getAmountOfQuestions " + e.getMessage());
        }

        return -1;
    }

    // Retorna pergunta com o id passado como parametro
    // Coloca na variavel currentAnswer a resposta da pergunta que foi buscada
    public static String getQuestionByID(int id){

        try {
            InputStream in = Reader.class.getClassLoader().getResourceAsStream("perguntas.txt");
            BufferedReader bf = new BufferedReader(new InputStreamReader(in));
            String temp = bf.readLine();

            while(temp != null) {
                if (temp.equals(String.format("ID: %d", id))) break;
                temp = bf.readLine();
            }

            String res = "";

            while(!temp.equals("Fim Pergunta")) {
                String next = bf.readLine();
                if(next.contains("Fim Pergunta")) break;
                res += "\n" + next;
            }

            currentAnswer = bf.readLine().substring(10);


            bf.close();
            return res;


        } catch (Exception e){
            System.out.println("Problema ao carregar arquivo em getQuestionByID");
        }
        return null;
    }

    // Coloca em "questionsArray" uma certa quantidade de perguntas aleatorias e suas respectivas respostas em "answersArray"
    // A quantidade de perguntas a serem buscadas e passada no parametro perguntasPorQuiz
    public static void generateRandomQuestions(int perguntasPorQuiz){
        questionsArray = new String[perguntasPorQuiz];
        answersArray = new String[perguntasPorQuiz];

        // Cria lista com ID's aleatorios que serao buscados como perguntas
        ArrayList<Integer> randomIds = new ArrayList<>();
        for (int i = 1; i <= getAmountOfQuestions(); i++) randomIds.add(i);
        Collections.shuffle(randomIds);

        // Busca perguntas referentes aos ID's na lista "randomIds"
        for(int i = 0; i < perguntasPorQuiz; i++){
            questionsArray[i] = Reader.getQuestionByID(randomIds.get(i));
            answersArray[i] = currentAnswer;
        }
    }


    // -- Getters e Setters -- //

    public static String getAnwser(){
        return currentAnswer;
    }
}