import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class Client {
    private static String HOST;
    private static int PORT;
    private static String readAnswer(Scanner scanner){
        return scanner.nextLine();
    }

    public static void main(String[] args) {
        loadConfig();

        try (
                Socket socket = new Socket(HOST, PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in)); // wpisy od usera
        ) {
            // Odbierz i pokaż unikatowy identyfikator
            String idLine = in.readLine();
            System.out.println(idLine);

            String line;
            while((line = in.readLine()) != null){
                // Jeśli serwer wyśle wynik lub komunikat o rezygnacji:
                if(line.startsWith("Twój wynik") || line.startsWith("Zrezygnowano")){
                    System.out.println(line);
                    break;
                }

                // Wyświetlanie pytania
                System.out.println(line);

                // Wyświetlanie odpowiedzi
                for(int i = 0; i < 4; i++){
                    String option = in.readLine();
                    if(option != null){
                        System.out.println(option);
                    }
                }

                // Odczyt odpowiedzi od uzytkownika
                System.out.print("Odpowiedź (A/B/C/D lub Q by zakończyć): ");
                String answer = "";
                long start = System.currentTimeMillis();

                while(System.currentTimeMillis() - start < 30000){
                    if(userInput.ready()){
                        String input = userInput.readLine().trim();
                        if(input.equalsIgnoreCase("Q")){
                            out.println("Q");
                            System.out.println(in.readLine()); // komunikat o rezygnacji
                            System.out.println(in.readLine()); // wynik
                            return;
                        } else if(input.matches("(?i)[A-D]")){
                            answer = input.toUpperCase();
                            break;
                        } else{
                            System.out.println("Nieprawidłowa odpowiedź. Dozwolone: A, B, C, D, Q");
                            System.out.print("Spróbuj ponownie: ");
                        }
                    } else {
                        Thread.sleep(100); // unikamy dużego obciążenia CPU
                    }
                }

                if(answer.isEmpty()){
                    System.out.println("Czas na odpowiedź minął.");
                    answer = "Timeout";
                }

                out.println(answer);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Błąd klienta: " + e.getMessage());
        }
    }

    // Metoda do wczytywania konfiguracji z pliku
    public static void loadConfig(){
        Properties props = new Properties();
        try(InputStream input = Client.class.getClassLoader().getResourceAsStream("config.properties")){
            if(input == null){
                System.err.println("Plik config.properties nie został znaleziony.");
                HOST = "localhost";
                PORT = 1234;
                return;
            }
            HOST = props.getProperty("host");
            PORT = Integer.parseInt(props.getProperty("port", "1234"));
        } catch (IOException e) {
            System.err.println("Błąd ładowania config.properties. Używam domyślnych wartości.");
            HOST = "localhost";
            PORT = 1234;
        }
    }
}
