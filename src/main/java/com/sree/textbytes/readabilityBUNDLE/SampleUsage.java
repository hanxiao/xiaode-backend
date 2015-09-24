package com.sree.textbytes.readabilityBUNDLE;

import com.sree.textbytes.network.HtmlFetcher;


public class SampleUsage {
	public static void main(String[] args) throws Exception {
		Article article = new Article();
		ContentExtractor ce = new ContentExtractor();
		HtmlFetcher htmlFetcher = new HtmlFetcher();
		String html = htmlFetcher.getHtml("http://www.bbc.com/zhongwen/simp/china/2015/09/150923_house_of_cards_china", 0);

		
		//System.out.println("Html : "+html);
		article = ce.extractContent(html, "ReadabilitySnack");


		
		System.out.println("Content : "+article.getCleanedArticleText());
		
		
	}

}
