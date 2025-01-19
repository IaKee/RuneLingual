package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.nonLatin.GeneralFunctions;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.List;
import java.util.ArrayList;

public class WidgetsUtilRLingual
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualPlugin plugin;
	@Inject
	private GeneralFunctions generalFunctions;
	private List<String> stringTranslatingInThread = new ArrayList<>();

	@Inject
	public WidgetsUtilRLingual(Client client, RuneLingualPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
	}

	public void setWidgetText_NiceBr(Widget widget, String newText) {
		if (newText.equals(widget.getText())) // the texts will be the same if the widget has already been translated, or doesn't have a translation available
			return;

		if (plugin.getConfig().getSelectedLanguage().needsCharImages())
			setWidgetText_NiceBr_CharImages(widget, newText);
		else
			setWidgetText_NiceBr_NoCharImages(widget, newText);
	}

	public void setWidgetText_NiceBr_apiTranslated(Widget widget, String newText) {

		if (newText.equals(widget.getText())) // the texts will be the same if the widget has already been translated, or doesn't have a translation available
			return;

		if (plugin.getConfig().getSelectedLanguage().needsCharImages())
			setWidgetText_NiceBr_CharImages(widget, newText);
		else
			setWidgetText_NiceBr_NoCharImages(widget, newText);
	}

	public void setWidgetText_ApiTranslation(Widget widget, String newText, Colors color){
		final String newText_withoutBrAndTags = removeBrAndTags(newText);
		if(stringTranslatingInThread.contains(newText)) // skip if already translating with api
			return;
		stringTranslatingInThread.add(newText);
		Thread thread = new Thread(() -> {
			try {
				String translatedText = plugin.getDeepl().translate(newText_withoutBrAndTags, LangCodeSelectableList.ENGLISH, plugin.getConfig().getSelectedLanguage());
				if (plugin.getTargetLanguage().needsCharImages()) {
					translatedText = generalFunctions.StringToTags(translatedText, color);
				}
				setWidgetText_NiceBr(widget, translatedText);
				stringTranslatingInThread.remove(newText);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		thread.setDaemon(false);
		thread.start();
	}

	public void setWidgetText_NiceBr_CharImages(Widget widget, String newText) { // todo: set to show overlay if the mouse is hovering and the widget is too small for the text to display
		// Set the text of the widget, but insert br considering the width of the widget
		int widgetWidth = widget.getWidth();
		int foreignWidth = plugin.getConfig().getSelectedLanguage().getCharWidth();
		int maxChars = widgetWidth / foreignWidth;

		// if language uses charImages and needs space between words
		if(plugin.getConfig().getSelectedLanguage().needsSpaceBetweenWords()) { // todo: test this when such language is added
			String[] words = newText.split(" ");
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(String word : words) {
				if(currentLineLength + word.length() > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(word);
				currentLineLength += word.length();
			}
			newText = newTextBuilder.toString();
		} else { // if language uses charImages and doesn't need space between words
			String[] letters = newText.split("(?<=>)");
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(String letter : letters) {
				if(currentLineLength + 1 > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(letter);
				currentLineLength++;
			}
			newText = newTextBuilder.toString();
		}
		widget.setText(newText);
	}

	public void setWidgetText_NiceBr_NoCharImages(Widget widget, String newText) {
		// Set the text of the widget, but insert br considering the width of the widget
		int widgetWidth = widget.getWidth();
		int foreignWidth = plugin.getConfig().getSelectedLanguage().getCharWidth();
		int maxChars = widgetWidth / foreignWidth;

		if(plugin.getConfig().getSelectedLanguage().needsSpaceBetweenWords()) {
			String[] words = newText.split(" ");
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(String word : words) {
				if(currentLineLength + word.length() > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(word);
				currentLineLength += word.length();
			}
			newText = newTextBuilder.toString();
		} else {
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(int i = 0; i < newText.length(); i++) {
				if(currentLineLength + 1 > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(newText.charAt(i));
				currentLineLength++;
			}
			newText = newTextBuilder.toString();
		}
		widget.setText(newText);
	}

	public static String removeBrAndTags(String str) {
		// replaces br with space
		String tmp = str.replaceAll("(?<=\\S)<br>(?=\\S)", " ");
		return Colors.removeColorTag(tmp);
	}

	public Widget[] getAllChildren()
	{
		// Create a list to store widgets
		List<Widget> widgetList = new ArrayList<>();

		Widget[] roots = client.getWidgetRoots();
		for (Widget root : roots) {
			iterateWidgetsRecursive(root, widgetList);
		}

		// Convert the list to an array and return
		return widgetList.toArray(new Widget[0]);
	}


	public static List<Widget> getAllChildren(Widget widget)
	{
		// Create a list to store widgets
		List<Widget> widgetList = new ArrayList<>();
		
		// Call the recursive method to populate the list
		iterateWidgetsRecursive(widget, widgetList);
		
		// Convert the list to an array and return
		return widgetList;
	}
	
	private static void iterateWidgetsRecursive(Widget widget, List<Widget> widgetList)
	{
		// Check if the widget is not null and not hidden
		if(widget != null && !widget.isHidden())
		{
			// Add the widget to the list
			if(!widget.getText().isBlank() || !widget.getName().isBlank())
			{
				widgetList.add(widget);
			}
			
			Widget[] staticChildren = widget.getStaticChildren();
			if (staticChildren != null)
			{
				for (Widget child : staticChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
			Widget[] dynamicChildren = widget.getDynamicChildren();
			if (dynamicChildren != null)
			{
				for (Widget child : dynamicChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
			Widget[] nestedChildren = widget.getNestedChildren();
			if (nestedChildren != null)
			{
				for (Widget child : nestedChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
		}
	}
}
