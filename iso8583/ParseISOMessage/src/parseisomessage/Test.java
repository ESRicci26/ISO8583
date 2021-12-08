package parseisomessage;

import java.io.IOException;
import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOServer;
import org.jpos.iso.ISOSource;
import org.jpos.iso.ServerChannel;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;
import org.jpos.util.ThreadPool;

public class Test {

    public static void main(String[] args) throws IOException, ISOException {

        Logger l = new Logger();
        l.addListener(new SimpleLogListener());
        GenericPackager serverPkg = new GenericPackager(
                "E:\\ISO\\fields.xml");
        serverPkg.setLogger(l, "Server"); // Para que a saída possa ser diferenciada com base no domínio

        GenericPackager clientPkg = new GenericPackager(
                "E:\\ISO\\fields.xml");
        clientPkg.setLogger(l, "Client");// Para que a saída possa ser diferenciada com base no domínio
        // Simular um servidor e escutar em uma porta
        ISOChannel serverChannel = new ASCIIChannel(serverPkg);
        ((ASCIIChannel) serverChannel).setHeader("ISO70100000");
        // AN equivalent in your channel adaptor deploy file would be
        // <channel class="org.jpos.iso.channel.ASCIIChannel"
        // packager="org.jpos.iso.packager.GenericPackager"
        // header="ISO70100000"> .....
        // This is evident from the ChanelAdaptor code
        // QFactory.invoke (channel, "setHeader", e.getAttributeValue ("header"));
        ((ASCIIChannel) serverChannel).setLogger(l, "server");
        ISOServer server = new ISOServer(7654, (ServerChannel) serverChannel,
                new ThreadPool(10, 100, "serverListeningThread"));

        server.addISORequestListener(new ISORequestListener() {
            // Se o cliente enviar uma mensagem, o servidor responderá e aprovará se for uma mensagem de solicitação
            @Override
            public boolean process(ISOSource source, ISOMsg msg) {
                try {
                    if (!msg.isRequest()) {
                        msg.setResponseMTI();
                        msg.set(39, "000");
                        source.send(msg);
                    }
                }
                catch (ISOException | IOException ex) {

                }

                return true;
            }
        });
        Thread serverThread = new Thread(server);
        serverThread.start(); // Além deste ponto, o servidor está ouvindo uma conexão do cliente

        ASCIIChannel clientChannel = new ASCIIChannel("127.0.0.1", 7654, clientPkg);
        //clientChannel.setHeader("ISO70100000");​ //Semelhante ao servidor, você pode configurar a constante em seu arquivo de implantação
        clientChannel.setLogger(l, "client");
        clientChannel.connect(); // Conectar ao servidor, será visto no console de saída
        ISOChannel connectChannel = server.getLastConnectedISOChannel();// Uma vez que o servidor pode ter várias conexões, temos o último que está conectado a ele.

        ISOMsg serverInitiatedRequest = new ISOMsg();

        serverInitiatedRequest.set(0, "1804");
        serverInitiatedRequest.set(7, "1607161705");
        serverInitiatedRequest.set(11, "888402");
        serverInitiatedRequest.set(12, "160716170549");
        serverInitiatedRequest.set(24, "803");
        serverInitiatedRequest.set(25, "0000");
        serverInitiatedRequest.set(33, "101010");
        serverInitiatedRequest.set(37, "619817888402");

        connectChannel.send(serverInitiatedRequest); // Use o último conectado para enviar uma mensagem de solicitação ao cliente.
        ISOMsg receivedRequest = clientChannel.receive();// Receber a mensagem de solicitação do servidor no cliente

        ISOMsg clientResponse = (ISOMsg) receivedRequest.clone();
        clientResponse.setResponseMTI();
        clientResponse.set(39, "000");
        clientChannel.send(clientResponse); // Enviar a resposta para o servidor

    }

}



/*

package parseisomessage;

import java.io.IOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOFilter;
import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.channel.LoopbackChannel;
import org.jpos.util.LogEvent;

public class Test implements ISOFilter {

public static void main (String[] args) {
try {
new Test().run();
} catch (Exception e) {
e.printStackTrace();
}
}

public void run () throws ISOException, IOException {
LoopbackChannel channel = new LoopbackChannel ();
channel.addIncomingFilter (this);
ISOMsg request = createRequest();
request.dump (System.out, "request> ");
channel.send (request);
ISOMsg response = channel.receive();
response.dump (System.out, "response> ");
}

private ISOMsg createRequest () throws ISOException {
ISOMsg m = new ISOMsg ("0800");
m.set (11, "000001");
m.set (41, "29110001");
m.set (70, "301");
return m;
}

public ISOMsg filter (ISOChannel channel, ISOMsg m, LogEvent evt) {
try {
m.setResponseMTI();
m.set (39, "00");
//m.setResponseMTI();
//m.set (13, "00");
} catch (ISOException e) {
e.printStackTrace();
}
return m;
}
}



package parseisomessage;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

import java.io.InputStream;

public class ParseISOMessage {
    public static void main(String[] args) {
        ParseISOMessage iso = new ParseISOMessage();
        try {
        String message = "02003220000000808000000010000000001500120604120000000112340001840";
            ISOMsg isoMsg = iso.parseISOMessage(message);
            iso.printISOMessage(isoMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ISOMsg parseISOMessage(String message) throws Exception {
        System.out.printf("Mensagem RICCI = %s%n", message);
        try {
            // Load package from resources directory.
            InputStream is = getClass().getResourceAsStream("C:\\Users\\ISO\\fields.xml");
            GenericPackager packager = new GenericPackager(is);
            ISOMsg isoMsg = new ISOMsg();
            isoMsg.setPackager(packager);
            isoMsg.unpack(message.getBytes());
            return isoMsg;
        } catch (ISOException e) {
            throw new Exception(e);
        }
    }

    private void printISOMessage(ISOMsg isoMsg) {
        try {
            System.out.printf("MTI = %s%n", isoMsg.getMTI());
            for (int i = 1; i <= isoMsg.getMaxField(); i++) {
                if (isoMsg.hasField(i)) {
                    System.out.printf("Field (%s) = %s%n", i, isoMsg.getString(i));
                }
            }
        } catch (ISOException e) {
            e.printStackTrace();
        }
    }
}

*/

