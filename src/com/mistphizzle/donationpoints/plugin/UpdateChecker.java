package com.mistphizzle.donationpoints.plugin;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UpdateChecker {

	private static DonationPoints plugin;
	private static URL filesFeed;

	private static String version;
	private static String link;

	public UpdateChecker(DonationPoints plugin, String url) {
		UpdateChecker.plugin = plugin;

		try {
			UpdateChecker.filesFeed = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public static boolean updateNeeded() {
		try {
			InputStream input = filesFeed.openConnection().getInputStream();
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);

			Node latestFile = document.getElementsByTagName("item").item(0);
			NodeList children = latestFile.getChildNodes();

			version = children.item(1).getTextContent().replaceAll("[a-zA-Z ]", "");
			link = children.item(3).getTextContent();

			if (!plugin.getDescription().getVersion().equals(version)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getVersion() {
		return UpdateChecker.version;
	}

	public String getLink() {
		return UpdateChecker.link;
	}
}