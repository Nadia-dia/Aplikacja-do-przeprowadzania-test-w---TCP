public class Question {
    private final String text;
    private final String[] options;
    private final String correct;

    public Question(String text, String[] options, String correct) {
        this.text = text;
        this.options = options;
        this.correct = correct;
    }

    public String formatForSending() { // wyslanie do klienta
        return text + "\n" + String.join("\n", options);
    }

    public boolean isCorrect(String answer) {
        return correct.equalsIgnoreCase(answer.trim());
    }
}
