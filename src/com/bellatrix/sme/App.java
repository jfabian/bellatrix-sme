package com.bellatrix.sme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class App {

	public static void main(String[] args) {
		List<String> list = new ArrayList<>();
		
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Archivo del listado de sitios webs.");
		input.setRequired(true);
		options.addOption(input);
		
		Option output = new Option("o", "output", true, "Carpeta donde se guarda el resultado.");
		output.setRequired(true);
		options.addOption(output);
		
		Option method = new Option("m", "method", true, "Tipo de informacion a extraer [hashtag, username, propername].");
		method.setRequired(true);
		options.addOption(method);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			System.exit(1);
			return;
		}
		
		String inputUrlFile = cmd.getOptionValue("input");
		String outputFile = cmd.getOptionValue("output");
		String methodExtract = cmd.getOptionValue("method");
		
		String[] fieldsToInclude = new String[] {"hashtag","username","propername"};
		if (!Arrays.stream(fieldsToInclude).anyMatch(methodExtract::equals)) {
			System.out.println("Metodo '" + methodExtract + "' no soportado, los disponibles son hashtag, username y propername");
			System.exit(1);
			return;
		}
		
		try (Stream<String> stream = Files.lines(Paths.get(inputUrlFile))) {
			
			list = stream
				.parallel()
				.map(l -> {
					String content = "";
					try {
						URL obj = new URL(l);
						System.out.println("Analizando Web: " + l);
						HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
						conn.setReadTimeout(5000);
						conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
						conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:59.0) Gecko/20100101 Firefox/59.0");
						conn.addRequestProperty("Referer", "google.com");
						
						boolean redirect = false;
						
						int status = conn.getResponseCode();
						if (status != HttpURLConnection.HTTP_OK) {
							if (status == HttpURLConnection.HTTP_MOVED_TEMP
								|| status == HttpURLConnection.HTTP_MOVED_PERM
									|| status == HttpURLConnection.HTTP_SEE_OTHER)
							redirect = true;
						}
						
						if (redirect) {
							String newUrl = conn.getHeaderField("Location");
							conn = (HttpURLConnection) new URL(newUrl.trim()).openConnection();
							conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
							conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:59.0) Gecko/20100101 Firefox/59.0");
							conn.addRequestProperty("Referer", "google.com.pe");
						}
						BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String inputLine;
						StringBuffer html = new StringBuffer();
						List<String> lines_file = new ArrayList<String>();
						while ((inputLine = in.readLine()) != null) {
							html.append(inputLine);
						}
						content = html.toString();
						Document doc = Jsoup.parse(html.toString());
						content = doc.text();
						
						String pattern = "";
						switch(methodExtract) {
							case "hashtag" :
								pattern = "#(\\S+)";
								break;
							case "username" :
								pattern = "(?<=@)([\\\\w-]+)($|\\\\s)";
								break;
							case "propername" :
								pattern = "([A-ZÄËÏÖÜÁÉÍÓÚÂÊÎÔÛÀÈÌÒÙ][a-zäÄëËïÏöÖüÜáéíóúáéíóúÁÉÍÓÚÂÊÎÔÛâêîôûàèìòùÀÈÌÒÙ.-]{2,})";
								break;
							default :
								System.out.println("Metodo '" + methodExtract + "' no soportado, los disponibles son hashtag, username y propername");
						}
						
						if (!"".equals(pattern)) {
							Pattern MY_PATTERN = Pattern.compile(pattern);
							Matcher mat = MY_PATTERN.matcher(content);
							
							while (mat.find()) {
								lines_file.add(mat.group(1));
							}
							
							List<String> lines = lines_file;
							Path file = Paths.get(outputFile + "/" + l.replace(".", "-").replace("http://", "").replace("https://", "").replace("/", "") + ".txt");
							Files.write(file, lines, Charset.forName("UTF-8"));
							
							in.close();
						}
					} catch (Exception e) {
						System.out.println("No se pudo extraer la información de la web: " + l);
					}
					return content;
				})
				.collect(Collectors.toList());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
