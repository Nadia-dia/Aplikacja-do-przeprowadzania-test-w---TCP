import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int MAX_CLENTS = 250;
    private static List<Question> questions = new ArrayList<>();
    private static int port;
    private static final Object fileLock = new Object(); // do synchronizacji dostepu do plikow zapisu

    public static void main(String[] args) {
        loadConfig();
        loadQuestions("bazaPytan.txt");

        try{
            ServerSocket serverSocket = new ServerSocket(port);
            ExecutorService pool = Executors.newFixedThreadPool(MAX_CLENTS);

            while(true){
                Socket clientSocket = serverSocket.accept();
                pool.execute(()->handleClient(clientSocket));
            }

        }catch (IOException e){
            System.err.println("Blad serwera: " + e.getMessage());
        }
    }

    public static void loadConfig(){
        Properties props = new Properties();
        try(InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")){
            if(input != null){
                props.load(input);
                port = Integer.parseInt(props.getProperty("port", "1234"));
            } else {
                System.err.println("Nie znaleziono pliku config.properties, uzywam domyslnych wartosci");
                port = 1234;
            }
        } catch(IOException e){
            System.err.println("Blad ladowania pliku config.properties, uzywam domyslnych wartosci"
                    + e.getMessage());
            port = 1234;
        }
    }

    public static void loadQuestions(String filename){
        try(BufferedReader reader = new BufferedReader(new FileReader(filename))){
            while(true){
                String question = reader.readLine();
                if(question == null) break;

                String[] opts = new String[4];
                for(int i = 0; i < 4; i++){
                    opts[i] = reader.readLine();
                    if(opts[i] == null) return;
                }

                String correct = reader.readLine();

                questions.add(new Question(question, opts, correct));
                reader.readLine(); // pusta linia
            }
        } catch(IOException e){
            System.err.println("Błąd ładowania pytań: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void handleClient(Socket clientSocket){
        String clientId = UUID.randomUUID().toString();

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader((clientSocket.getInputStream())));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);)
        {
            List<String> userAnswers = new ArrayList<>();
            int score = 0;

            // Prześlij unikalny identyfikator klientowi
            out.println("Twój unikatowy identyfikator: " + clientId);

            for(Question q : questions){
                out.println(q.formatForSending());
                String response = in.readLine(); // Odczyt odpowiedzi od klienta

                if(response == null || response.equals("Timeout")|| response.trim().isEmpty()){
                    response = "Brak odpowiedzi (timeout)";
                }

                userAnswers.add(response);
                if(q.isCorrect(response)) score++;

                if(response.equalsIgnoreCase("Q")){
                    out.println("Zrezygnowano z testu przez użytkownika.");
                    break;
                }
            }

            out.println("Twój wynik: " + score + "/" + questions.size());
            saveResults(clientId, userAnswers, score);
        } catch (IOException e){
            System.err.println("Błąd klienta: " + e.getMessage());
        }
    }

    public static void saveResults(String clientId, List<String> answers, int score){
        synchronized (fileLock){
            try(
                FileWriter answersOut = new FileWriter("bazaOdpowiedzi.txt", true);
                FileWriter resultOut = new FileWriter("wyniki.txt", true);
            ){
                answersOut.write("[" + clientId + "] Odpowiedzi: " + answers + "\n");
                resultOut.write("[" + clientId + "] Wynik: " + score + "/" +questions.size() + "\n");
            } catch (IOException e){
                System.err.println("Błąd zapisu wyników: " + e.getMessage());
            }
        }
    }
}
