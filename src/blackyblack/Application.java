package blackyblack;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import blackyblack.http.APITestServlet;
import nrs.util.Logger;

public class Application
{
  private static final Properties defaultProperties = new Properties();

  static
  {
    try (InputStream is = ClassLoader.getSystemResourceAsStream("quack-default.properties"))
    {
      if (is != null)
      {
        Application.defaultProperties.load(is);
      }
      else
      {
        String configFile = System.getProperty("quack-default.properties");
        if (configFile != null)
        {
          try (InputStream fis = new FileInputStream(configFile))
          {
            Application.defaultProperties.load(fis);
          }
          catch (IOException e)
          {
            throw new RuntimeException("Error loading quack-default.properties from " + configFile);
          }
        }
        else
        {
          throw new RuntimeException("quack-default.properties not in classpath and system property quack-default.properties not defined either");
        }
      }
    }
    catch (IOException e)
    {
      throw new RuntimeException("Error loading quack-default.properties", e);
    }
  }

  private static final Properties properties = new Properties(defaultProperties);

  static
  {
    try (InputStream is = ClassLoader.getSystemResourceAsStream("quack.properties"))
    {
      if (is != null)
      {
        Application.properties.load(is);
      } // ignore if missing
    }
    catch (IOException e)
    {
      throw new RuntimeException("Error loading quack.properties", e);
    }
  }

  public static int getIntProperty(String name)
  {
    try
    {
      int result = Integer.parseInt(properties.getProperty(name));
      return result;
    }
    catch (NumberFormatException e)
    {
      return 0;
    }
  }

  public static String getStringProperty(String name)
  {
    return getStringProperty(name, null);
  }

  public static String getStringProperty(String name, String defaultValue)
  {
    String value = properties.getProperty(name);
    if (value != null && !"".equals(value))
    {
      return value;
    }
    else
    {
      return defaultValue;
    }
  }

  public static List<String> getStringListProperty(String name)
  {
    String value = getStringProperty(name);
    if (value == null || value.length() == 0)
    {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (String s : value.split(";"))
    {
      s = s.trim();
      if (s.length() > 0)
      {
        result.add(s);
      }
    }
    return result;
  }

  public static Boolean getBooleanProperty(String name)
  {
    String value = properties.getProperty(name);
    if (Boolean.TRUE.toString().equals(value))
    {
      return true;
    }
    else if (Boolean.FALSE.toString().equals(value))
    {
      return false;
    }
    return false;
  }

  public static final Set<String> allowedBotHosts;

  static
  {
    List<String> allowedBotHostsList = getStringListProperty("blackyblack.allowedBotHosts");
    if (!allowedBotHostsList.contains("*"))
    {
      allowedBotHosts = Collections.unmodifiableSet(new HashSet<>(allowedBotHostsList));
    }
    else
    {
      allowedBotHosts = null;
    }
  }

  public static final int port = getIntProperty("blackyblack.apiServerPort");
  public static final String host = getStringProperty("blackyblack.apiServerHost");
  public static final String defaultNrsHost = getStringProperty("blackyblack.nrsHost");
  public static final int defaultNrsPort = getIntProperty("blackyblack.nrsPort");
  public static final int defaultTestnetNrsPort = getIntProperty("blackyblack.testnetNrsPort");
  public static final String version;

  static
  {
    if (getBooleanProperty("blackyblack.isTestnet"))
    {
      version = AppConstants.APP_VERSION + "-test";
    }
    else
    {
      version = AppConstants.APP_VERSION;
    }
  }

  public static INxtApi api = new NxtApi();
  public static Boolean terminated = false;

  public static void main(String[] args)
  {
    start();
  }

  public static void start()
  {
    Logger.logMessage("Quack " + version);

    terminated = false;
    Server apiServer = new Server();
    ServerConnector connector = new ServerConnector(apiServer);
    connector.setPort(port);
    connector.setHost(host);
    connector.setIdleTimeout(getIntProperty("blackyblack.apiServerIdleTimeout"));
    connector.setReuseAddress(true);
    apiServer.addConnector(connector);

    HandlerList apiHandlers = new HandlerList();
    ServletContextHandler apiHandler = new ServletContextHandler();
    apiHandler.addServlet(APITestServlet.class, "/api");

    // allow CORS
    if (getBooleanProperty("blackyblack.apiServerCORS"))
    {
      FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
      filterHolder.setInitParameter("allowedHeaders", "*");
      filterHolder.setAsyncSupported(true);
    }

    apiHandlers.addHandler(apiHandler);
    apiServer.setHandler(apiHandlers);
    apiServer.setStopAtShutdown(true);
    try
    {
      apiServer.start();
    }
    catch (Exception e)
    {
      Logger.logMessage("Could not start API server", e);
      return;
    }

    Logger.logMessage("Started API server at " + host + ":" + port);

    while (true)
    {
      if (terminated)
        break;

      try
      {
        Logger.logMessage("NRS connection ready. Processing requests.");
        break;
      }
      catch (Exception e)
      {
        Logger.logWarningMessage("Cannot open NRS connection. Retrying in 30 seconds...");
      }

      int timeout = 0;

      while (true)
      {
        if (terminated)
          break;

        try
        {
          Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
          Logger.logWarningMessage("Quack terminated");
          terminated = true;
          break;
        }

        timeout++;

        // timeout expired, try again
        if (timeout > 30)
        {
          break;
        }
      }
    }

    try
    {
      while (true)
      {
        if (terminated)
          break;
        Thread.sleep(1000);
      }
    }
    catch (InterruptedException e)
    {
      Logger.logWarningMessage("Quack terminated");
      terminated = true;
    }

    try
    {
      apiServer.stop();
    }
    catch (Exception e)
    {
      Logger.logMessage("Could not stop API server", e);
    }

    Logger.logMessage("Quack stopped");
  }

  public static void stop()
  {
    Logger.logMessage("Stopping Quack...");
    terminated = true;
  }
}
