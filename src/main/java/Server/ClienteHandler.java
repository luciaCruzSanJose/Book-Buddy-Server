package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClienteHandler implements Runnable {

    private final Socket socket;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String isbn = in.readUTF();
            System.out.println("ISBN recibido: " + isbn);

            // Ejecutar scrapers
            AmazonScraper amazonScraper = new AmazonScraper();
            String amazonData = amazonScraper.scrapeBookData(isbn);

            CasaDelLibroScraper casaDelLibroScraper = new CasaDelLibroScraper();
            String casaDelLibroData = casaDelLibroScraper.scrapeBookData(isbn);

            LibreriaLucesScraper lucesScraper = new LibreriaLucesScraper();
            String lucesData = lucesScraper.scrapeBookData(isbn);

            // Combinar en un JSON final
            JSONObject resultadoFinal = new JSONObject();
            resultadoFinal.put("isbn", isbn);
            resultadoFinal.put("title", new JSONObject(amazonData).optString("titulo", "Título no disponible"));
            resultadoFinal.put("author", new JSONObject(amazonData).optString("autor", "Autor no disponible"));
            resultadoFinal.put("synopsis", new JSONObject(amazonData).optString("sinopsis", "Sinopsis no disponible"));
            resultadoFinal.put("cover_image", new JSONObject(amazonData).optString("imagen", ""));

            // Combinar géneros de Casa del Libro
            JSONArray generos = new JSONArray(new JSONObject(casaDelLibroData).optJSONArray("generos"));
            resultadoFinal.put("genre", generos);

            // Combinar precios de los tres scrapers
            JSONArray priceComparison = new JSONArray();

            // Amazon
            JSONObject amazonPrices = new JSONObject();
            amazonPrices.put("store", "Amazon");
            amazonPrices.put("url", new JSONObject(amazonData).optString("url", ""));
            amazonPrices.put("formats", new JSONObject(amazonData).optJSONArray("formatos"));
            priceComparison.put(amazonPrices);

            // Casa del Libro
            JSONObject casaDelLibroPrices = new JSONObject();
            casaDelLibroPrices.put("store", "Casa del Libro");
            casaDelLibroPrices.put("url", new JSONObject(casaDelLibroData).optString("url", ""));
            casaDelLibroPrices.put("formats", new JSONObject(casaDelLibroData).optJSONArray("formatos"));
            priceComparison.put(casaDelLibroPrices);

            // Librería Luces
            JSONObject lucesPrices = new JSONObject();
            lucesPrices.put("store", "Librería Luces");
            lucesPrices.put("url", new JSONObject(lucesData).optString("url", ""));
            lucesPrices.put("formats", new JSONObject(lucesData).optJSONArray("formatos"));
            priceComparison.put(lucesPrices);

            resultadoFinal.put("price_comparison", priceComparison);

            // Enviar respuesta al cliente
            out.writeUTF(resultadoFinal.toString());

        } catch (IOException e) {
            System.out.println("Error en la comunicación con el cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }
}
