package Server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;

public class AmazonScraper {
    public String scrapeBookData(String isbn) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless", "--disable-blink-features=AutomationControlled");

        WebDriver driver = new FirefoxDriver(options);
        JSONObject bookData = new JSONObject();

        try {
            driver.get("https://www.amazon.es/advanced-search/books");
            Thread.sleep(1000);

            // Rechazar cookies
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(4));
                WebElement botonRechazarCookies = wait
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("sp-cc-rejectall-link")));
                botonRechazarCookies.click();
            } catch (Exception e) {
                System.out.println("El banner de cookies no apareció o no pudo cerrarse: " + e.getMessage());
            }

            Thread.sleep(2000);

            // Buscar libro por ISBN
            try {
                WebElement campoIsbn = driver.findElement(By.id("field-isbn"));
                campoIsbn.sendKeys(isbn);
                WebElement botonBuscar = driver.findElement(By.xpath("//input[@type='image']"));
                botonBuscar.click();
                System.out.println("Búsqueda realizada para ISBN: " + isbn);
            } catch (Exception e) {
                return new JSONObject().put("error", "Error al buscar el libro por ISBN").toString();
            }

            Thread.sleep(2000);

            // Acceder a los detalles del libro
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement tituloEnlaceLibro = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[@class='a-link-normal s-line-clamp-2 s-link-style a-text-normal']")));
                tituloEnlaceLibro.click();
                System.out.println("Accediendo a la página del libro...");
            } catch (Exception e) {
                return new JSONObject().put("error", "No se pudo acceder al enlace del libro").toString();
            }

            Thread.sleep(1000);

            // Lista de formatos y precios
            JSONArray formatosArray = new JSONArray();
            try {
                List<WebElement> formatos = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions
                        .presenceOfAllElementsLocatedBy(By.xpath("//div[contains(@class, 'swatchElement')]")));

                for (WebElement formato : formatos) {
                    JSONObject formatoJson = new JSONObject();
                    try {
                        String titulo = formato.findElement(By.xpath(".//span[@class='slot-title']")).getText().trim();
                        String precio;
                        try {
                            precio = formato.findElement(By.xpath(".//span[contains(@class, 'slot-price')]")).getText()
                                    .trim().replace("€", "").trim(); // Eliminar el símbolo de euro
                        } catch (Exception ex) {
                            precio = "Precio no disponible";
                        }
                        formatoJson.put("format", titulo);
                        formatoJson.put("price", precio); // Asegúrate de que el precio sea un número
                        formatosArray.put(formatoJson);
                        System.out.println("Formato encontrado: " + titulo + " - Precio: " + precio);
                    } catch (Exception e) {
                        System.out.println("Error al procesar un formato: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                bookData.put("formatos", "No se pudieron obtener los formatos y precios");
                System.out.println("No se pudieron obtener los formatos y precios.");
            }
            bookData.put("formatos", formatosArray);

            // Leer más para ver toda la sinopsis
            try {
                WebElement textoLeerMas = driver.findElement(By.xpath(
                        "//div[@class='a-expander-collapsed-height a-row a-expander-container a-spacing-base a-expander-partial-collapse-container']//div[@class='a-expander-header a-expander-partial-collapse-header']//a[@class='a-declarative']//span[@class='a-expander-prompt'][contains(text(),'Leer más')]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", textoLeerMas);
                textoLeerMas.click();
                System.out.println("Se ha expandido la sinopsis.");
            } catch (Exception e) {
                System.out.println("El botón 'Leer más' no está presente o no pudo expandirse: " + e.getMessage());
            }

            // Extraer la sinopsis
            try {
                WebElement textoSinopsis = driver.findElement(By.id("bookDescription_feature_div"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", textoSinopsis);
                bookData.put("sinopsis", textoSinopsis.getText());
                System.out.println("Sinopsis obtenida: " + textoSinopsis.getText());
            } catch (Exception e) {
                bookData.put("sinopsis", "No disponible");
                System.out.println("No se pudo obtener la sinopsis.");
            }

            // Extraer autor
            try {
                WebElement enlaceAutor = driver.findElement(By.xpath("//span[@class='author notFaded']//a"));
                bookData.put("autor", enlaceAutor.getText());
                System.out.println("Autor obtenido: " + enlaceAutor.getText());
            } catch (Exception e) {
                bookData.put("autor", "No disponible");
                System.out.println("No se pudo obtener el autor.");
            }

            // Extraer título del libro
            try {
                WebElement tituloLibro = driver.findElement(By.xpath("//span[@id='productTitle']"));
                bookData.put("titulo", tituloLibro.getText());
                System.out.println("Título obtenido: " + tituloLibro.getText());
            } catch (Exception e) {
                bookData.put("titulo", "No disponible");
                System.out.println("No se pudo obtener el título.");
            }

            // URL del libro
            try {
                String urlLibro = driver.getCurrentUrl();
                bookData.put("url", urlLibro);
                System.out.println("URL del libro: " + urlLibro);
            } catch (Exception e) {
                bookData.put("url", "No disponible");
                System.out.println("No se pudo obtener la URL del libro.");
            }

            // Extraer la imagen del libro
            try {
                WebElement imagenLibro = driver.findElement(By.id("landingImage"));
                String urlImagen = imagenLibro.getAttribute("src");
                bookData.put("imagen", urlImagen);
            } catch (Exception e) {
                bookData.put("imagen", "No disponible");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("error", "Error general en el scraping").toString();
        } finally {
            driver.quit();
        }

        return bookData.toString();
    }
}
