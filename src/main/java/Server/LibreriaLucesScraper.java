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

public class LibreriaLucesScraper {

    public static String scrapeBookData(String isbn) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless", "--disable-blink-features=AutomationControlled");
        WebDriver driver = new FirefoxDriver(options);

        JSONObject jsonLibro = new JSONObject();

        try {
            driver.get("https://www.librerialuces.com/es/");
            Thread.sleep(3000);
            comprobarAparicionBannerSuscripcion(driver);

            // Manejo de cookies
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement enlaceConfiguracion = wait
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("cookie-compliant")));
                enlaceConfiguracion.click();
                Thread.sleep(1000);

                WebElement botonCerrarCookies = driver.findElement(By.id("grabarAceptar"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", botonCerrarCookies);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonCerrarCookies);
                System.out.println("Cookies rechazadas.");
            } catch (Exception e) {
                System.out.println("No se pudo interactuar con el botón de cookies: " + e.getMessage());
            }

            // Buscar libro por ISBN
            WebElement campoBusqueda = driver.findElement(By.xpath("//form[@id='busqueda']//input[@type='text']"));
            campoBusqueda.sendKeys(isbn);
            Thread.sleep(1000);

            WebElement botonBuscar = driver.findElement(By.xpath("//button[@type='submit']"));
            botonBuscar.click();
            Thread.sleep(2000);

            // Seleccionar el primer resultado
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement listaLibros = wait
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//ul[@class='books grid']")));
                comprobarAparicionBannerSuscripcion(driver);

                WebElement tituloPrimero = listaLibros.findElement(By.xpath(".//li[1]//dd[@class='title']/a"));
                tituloPrimero.click();
            } catch (Exception e) {
                System.out.println("Error obteniendo el título del primer libro: " + e.getMessage());
            }

            Thread.sleep(1000);
            comprobarAparicionBannerSuscripcion(driver);

            // Extraer detalles del libro
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//dl[@class='summary']")));

                // Extraer el precio
                String precio = "";
                try {
                    precio = driver.findElement(By.xpath("//div[@class='precioDetalle']//span[@class='despues']"))
                            .getText().trim().replace("€", "").trim(); // Eliminar el símbolo de euro
                } catch (Exception e) {
                    System.out.println("No se pudo obtener el precio: " + e.getMessage());
                    precio = "Precio no disponible";
                }

                // Agregar los datos al JSON
                jsonLibro.put("url", driver.getCurrentUrl());

                // Crear un formato "Normal" para el precio
                JSONArray formatosArray = new JSONArray();
                JSONObject formatoNormal = new JSONObject();
                formatoNormal.put("format", "Normal");
                formatoNormal.put("price", precio); // Guardar el precio en el formato
                formatosArray.put(formatoNormal);
                jsonLibro.put("formatos", formatosArray); // Guardar el array de formatos

                System.out.println("URL: " + driver.getCurrentUrl());
                System.out.println("Precio: " + precio);

            } catch (Exception e) {
                System.out.println("Error al extraer los datos: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return jsonLibro.toString();
    }

    private static void comprobarAparicionBannerSuscripcion(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            wait.until(ExpectedConditions
                    .presenceOfElementLocated(By.xpath("//div[@class='modalContent__content strictContent m']")));
            WebElement botonCerrarSuscripcion = driver.findElement(By.xpath("//button[@class='mc-closeModal']"));
            botonCerrarSuscripcion.click();
            System.out.println("Banner de suscripción cerrado.");
        } catch (Exception e) {
            System.out.println("El banner de suscripción no apareció o no pudo cerrarse: " + e.getMessage());
        }
    }
}
