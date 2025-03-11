package Server;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.util.List;

public class CasaDelLibroScraper {
    public String scrapeBookData(String isbn) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless", "--disable-blink-features=AutomationControlled");

        WebDriver driver = new FirefoxDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JSONObject bookData = new JSONObject();

        try {
            driver.get("https://www.casadellibro.com/librosdetexto");
            Thread.sleep(2000);

            // Rechazar Cookies
            try {
                WebElement botonCookies = wait
                        .until(ExpectedConditions.elementToBeClickable(By.id("onetrust-reject-all-handler")));
                botonCookies.click();
                System.out.println("Cookies rechazadas.");
                Thread.sleep(1500);
            } catch (Exception e) {
                System.out.println("No apareció el banner de cookies.");
            }

            // Buscar libro
            WebElement campoBusqueda = wait
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@rows='4']")));
            campoBusqueda.sendKeys(isbn);
            WebElement botonBuscar = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'btn') and contains(@class, 'accent')]")));
            botonBuscar.click();
            System.out.println("Búsqueda realizada con ISBN: " + isbn);

            Thread.sleep(3000);

            // Enlace del libro
            WebElement enlaceLibro = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(@class, 'product')]//a[1]")));
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", enlaceLibro);
            Thread.sleep(500);
            enlaceLibro.click();
            System.out.println("Accediendo a la página del libro...");

            Thread.sleep(2000);

            // Extraer géneros
            JSONArray generosArray = new JSONArray();
            try {
                List<WebElement> generos = driver.findElements(
                        By.xpath("//a[contains(@class, 'f-size-00 mr-2 py-1 px-2 border-r-1 svelte-jdwnnr')]"));
                for (WebElement genero : generos) {
                    generosArray.put(genero.getText().trim());
                    System.out.println("Género: " + genero.getText().trim());
                }
            } catch (Exception e) {
                System.out.println("No se pudieron obtener los géneros.");
            }

            // URL del libro
            String urlLibro = driver.getCurrentUrl();
            System.out.println("URL del libro: " + urlLibro);
            bookData.put("url", urlLibro);

            // Extraer formatos y precios
            JSONArray formatosArray = new JSONArray();
            try {
                List<WebElement> formatos = driver.findElements(By.xpath(
                        "//div[contains(@class, 'formatos')]//p[contains(@class, 'producto-asociado')] | //div[contains(@class, 'formatos')]//a[contains(@class, 'producto-asociado')]"));

                for (WebElement formato : formatos) {
                    JSONObject formatoJson = new JSONObject();
                    try {
                        String nombreFormato = formato.findElement(By.xpath(".//p[contains(@class, 'f-w-5')]"))
                                .getText().trim();
                        String precio;
                        try {
                            precio = formato.findElement(By.xpath(".//p[2]")).getText().trim().replace("€", "").trim(); // Eliminar
                            // el
                            // símbolo
                            // de
                            // euro
                        } catch (Exception ex) {
                            precio = "Precio no disponible";
                        }
                        formatoJson.put("format", nombreFormato);
                        formatoJson.put("price", precio); // Asegúrate de que el precio sea un número
                        formatosArray.put(formatoJson);
                        System.out.println("Formato: " + nombreFormato + " - Precio: " + precio);
                    } catch (Exception e) {
                        System.out.println("Error al procesar un formato: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                bookData.put("formatos", "No se pudieron obtener los formatos y precios");
                System.out.println("No se pudieron obtener los formatos y precios.");
            }
            bookData.put("formatos", formatosArray);
            bookData.put("generos", generosArray);

        } catch (Exception e) {
            System.out.println("Error durante el scraping: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return bookData.toString();
    }
}
