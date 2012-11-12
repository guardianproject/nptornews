package org.npr.api;

import android.content.Context;
import android.util.Log;
import org.apache.http.client.ClientProtocolException;
import org.npr.android.util.NodeUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Book extends ApiElement {
  private static final String LOG_TAG = org.npr.api.Book.class.getName();

  public Book(String id) {
    super(id);
  }

  private String title;
  private String promoArt;
  private String author;
  private String text;

  public String getTitle() {
    return title;
  }

  public String getPromoArt() {
    return promoArt;
  }

  public String getAuthor() {
    return author;
  }

  public String getText() {
    return text;
  }

  private static Book parseBook(Node memberNode, String id) {
    Book book = new Book(id);
    for (Node node : new IterableNodeList(memberNode.getChildNodes())) {
      String nodeName = node.getNodeName();
      if (nodeName.equals("title")) {
        book.title = NodeUtils.getTextContent(node);

      } else if (nodeName.equals("promoArt")) {
        Attr refIdAttr = (Attr)node.getAttributes().getNamedItem("refId");
        if (refIdAttr != null) {
          book.promoArt = refIdAttr.getValue();
        }

      } else if (nodeName.equals("author")) {
        for (Node authorNode : new IterableNodeList(node.getChildNodes())) {
          if (authorNode.getNodeName().equals("title")) {
            if (book.author != null) {
              book.author += ", " + NodeUtils.getTextContent(authorNode);
            } else {
              book.author = NodeUtils.getTextContent(authorNode);
            }
            break;
          }
        }

      } else if (nodeName.equals("introText")) {
        book.text = NodeUtils.getTextContent(node);
      }
    }

    return book;
  }

  private static List<Book> parseStory(Node storyNode) {

    Hashtable<String, Book> books = new Hashtable<String, Book>();
    Hashtable<String, String> promoArts = new Hashtable<String, String>();
    Hashtable<Integer, String> collection = new Hashtable<Integer, String>();

    NodeList storyList = storyNode.getChildNodes();
    for (Node node : new IterableNodeList(storyList)) {
      String nodeName = node.getNodeName();

      if (nodeName.equals("promoArt")) {
        Attr idAttr = (Attr) node.getAttributes().getNamedItem("id");
        Attr srcAttr = (Attr) node.getAttributes().getNamedItem("src");
        if (idAttr != null && srcAttr != null) {
          promoArts.put(idAttr.getValue(), srcAttr.getValue());
        }

      } else if (nodeName.equals("collection")) {
        for (Node collectionNode : new IterableNodeList(node.getChildNodes())) {
          if (collectionNode.getNodeName().equals("member")) {
            Attr refIdAttr = (Attr) collectionNode.getAttributes().getNamedItem("refId");
            Attr numAttr = (Attr) collectionNode.getAttributes().getNamedItem("num");
            if (refIdAttr != null && numAttr != null) {
              collection.put(Integer.parseInt(numAttr.getValue()), refIdAttr.getValue());
            }
          }
        }

      } else if (nodeName.equals("member")) {
        Attr idAttr = (Attr) node.getAttributes().getNamedItem("id");
        books.put(idAttr.getValue(), parseBook(node, idAttr.getValue()));
      }
    }

    if (books.size() == 0) {
      return null;
    } else {
      ArrayList<Book> result = new ArrayList<Book>(books.size());
      for (int i = 1 ; i <= collection.size() ; i++) {
        String id = collection.get(i);
        if (id != null) {
          Book book = books.get(id);
          if (book != null) {
            if (book.promoArt != null) {
              book.promoArt = promoArts.get(book.promoArt);
            }
            result.add(book);
          }
        }
      }
      return result;
    }
  }

  private static List<Book> parseBooks(Node rootNode, String storyId) {

    for (Node listNode : new IterableNodeList(rootNode.getChildNodes())) {
      if (listNode.getNodeName().equals("list")) {

        for (Node storyNode : new IterableNodeList(listNode.getChildNodes())) {
          if (storyNode.getNodeName().equals("story")) {

            Attr idAttr = (Attr) storyNode.getAttributes().getNamedItem("id");
            if (idAttr.getValue().equals(storyId)) {
              return parseStory(storyNode);
            }
          }
        }
      }
    }

    return null;
  }

  public static List<Book> downloadBooks(String url, String storyId, Context context) {
    if (url == null || storyId == null)
      return null;

    Node books = null;
    try {
      books = new Client(url, context).execute();
    } catch (ClientProtocolException e) {
      Log.e(LOG_TAG, "", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
    } catch (SAXException e) {
      Log.e(LOG_TAG, "", e);
    } catch (ParserConfigurationException e) {
      Log.e(LOG_TAG, "", e);
    }

    if (books == null) {
      return null;
    }
    return parseBooks(books, storyId);
  }
}
