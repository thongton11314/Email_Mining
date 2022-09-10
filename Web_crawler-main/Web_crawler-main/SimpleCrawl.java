import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class of simple hoping URL. This class contain functions that using the
 * initial URL to jump to the next available URL.
 */
public class SimpleCrawl {

    // DEFINE numbers
    final static int GET_URL = 0;
    final static int GET_HOP_NUMBER = 1;

    // DATA stucture use to store visited URL
    public static Set<URL> visitedURL = new HashSet<URL>();

    /**
     * main function to run the URL hoping
     * 
     * @param args Two parameters must provide to run application appropriately
     * @throws IOException        Handle bad given input
     * @throws URISyntaxException Handle bad taken URL
     */
    public static void main(String[] args) throws IOException,   URISyntaxException {

        // Main hoping
        try {

            // Must have two arguments
            if (args.length != 2) {
                System.out.println("Error: must give exactly two arguments");
                return; // End program if now qualify 2 arguments
            }

            // Parse in the arguments
            int nHops = Integer.parseInt(args[GET_HOP_NUMBER]);

            // Early exit
            // Bad number of hops
            if (nHops < 0) {
                throw new Exception("Second argument must be positive number");
            }
            if (nHops == 0) {
                System.out.println("Terminating by hop equal 0");
                return;
            }

            // Do hoping until reach the number of hope
            int currentHop;
            URL curURL = new URL(args[GET_URL]);
            URL nextURL = curURL;

            // Early exit
            // First URL fail to access
            int numResponse = checkResponse(curURL);
            if (numResponse >= 400) {
                throw new IOException("Cannot access the page, response " + numResponse);
            }

            for (currentHop = 1; currentHop <= nHops; currentHop++) {

                // Current URL is now next URL
                curURL = nextURL;

                // Print to the console current URL
                System.out.println("URL hopping" + "(" + currentHop + "): " + curURL);

                // Add the current URL in to list of URL
                visitedURL.add(curURL);

                // Get net page, after this statement, currentURL have new value
                nextURL = getNextURL(curURL);

                // Terminate if current and prevous are identical, no more access
                if (nextURL.equals(curURL)) {
                    System.out.println("Warning: no more available pages after hop " + currentHop);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (URISyntaxException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Check the response of the URL and return the response number
     * 
     * @param curURL Provided URL that being check
     * @return Response number of the provided URL
     * @throws URISyntaxException Handle bad URL
     * @throws IOException        Handle bad input
     */
    public static int checkResponse(URL curURL) throws URISyntaxException, IOException {
        HttpURLConnection connection = (HttpURLConnection) curURL.openConnection();
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(9000);
        int statusCode = connection.getResponseCode();
        return statusCode;
    }

    /**
     * This function will perform the redirection responde and return the new link
     * 
     * @param curURL current URL link
     * @return new URL link
     * @throws URISyntaxException Handle bad URI
     * @throws IOException        Handle In/Out exception
     */
    public static String redirection(URL curURL) throws URISyntaxException, IOException {
        HttpURLConnection newConnection = (HttpURLConnection) curURL.openConnection();
        String location = newConnection.getHeaderField("Location");
        return location;
    }

    /**
     * This function will take care of trailing. If have back_slash, add to HashSet
     * similar link without bach_slash. By this way, the link with trailing can be
     * treated as without trailing.
     * 
     * @param stringURL string of URL
     */
    public static void updateTrailing(URL currURL) throws URISyntaxException, IOException {
        String stringURL = currURL.toString();

        // with trailing
        if (Character.compare(stringURL.toString().charAt(stringURL.length() - 1), '/') == 0) {
            visitedURL.add(new URL(stringURL.substring(0, stringURL.length() - 1)));
            return;
        }
    }

    /**
     * Get the next URL from the given URL and return the next URL. The next URL
     * return never be the same as visited or error URL.
     * 
     * @param curURL URL that use as benchmark to look for the next URL
     * @return The next URL of given URL
     * @throws URISyntaxException Handle bad URL
     * @throws IOException        Handle bad Input
     */
    public static URL getNextURL(URL curURL) throws URISyntaxException, IOException {

        // Download the current URL page
        // Assign source page into buffer
        HttpURLConnection connection = (HttpURLConnection) curURL.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // Read every line of source page for looking https and http pattern
        String line;
        while ((line = reader.readLine()) != null) {

            // Define the pattern
            Pattern p = Pattern.compile("[<]a .*? ?href=\"(http[s]?://(.*?))\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            var m = p.matcher(line);

            // Find the https/http pattern
            while (m.find()) {

                // Get only https/http string
                String tempURL = m.group(1);

                // Create a URL after tokenize the string
                URL newURL = new URL(tempURL);
                
                // Check web response
                int numResponseURL = checkResponse(newURL);

                // Redirect response
                if (numResponseURL >= 300 && numResponseURL < 400) {
                    newURL = new URL((redirection(newURL)));
                }

                // 4xx response
                if (numResponseURL >= 400) {
                    continue;
                }

                // Check if the URL not visit
                if (!visitedURL.contains(newURL)) {
                    visitedURL.add(newURL);
                    return newURL;
                }
                // Keep running if visit
            }
        }

        // No more website to access
        // Return the current web
        return curURL;
    }
}