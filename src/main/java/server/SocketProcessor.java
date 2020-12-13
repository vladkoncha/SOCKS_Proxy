package server;

import message.Message;
import socks.SocksConsts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class SocketProcessor {

    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Message message = (Message) key.attachment();
        if (message == null) {
            key.attach(message = new Message());
            message.setIn(ByteBuffer.allocate(Message.BUF_SIZE));
            readGreeting(key, channel, message);
        }
        if (channel.read(message.getIn()) < 1) {
            close(key);
        } else if (message.getPeer() == null) {
            readHeader(key, message);
        }
    }

    public void write(SelectionKey key) throws IOException {
        Message message = (Message) key.attachment();

        if ((message.getStep() == Message.Step.GREETING) || (message.getStep() == Message.Step.ERROR)) {
            writeServerChoice(key, message);
        }

    }

    private void writeServerChoice(SelectionKey key, Message message) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        if (message.getOut() == null) {
            byte[] choice = new byte[]{SocksConsts.VER, SocksConsts.NO_AUTH};
            message.setOut(ByteBuffer.wrap(choice));
        }
        if ((message.getStep() == Message.Step.GREETING)
                || (message.getStep() == Message.Step.ERROR)) {
            channel.write(message.getOut());
        }
        if (message.getOut().remaining() == 0) {
            System.out.println("Sent choice: " + channel.socket());
            message.setStep(Message.Step.CONNECTION);
            key.interestOps(SelectionKey.OP_READ);
            key.attach(message);
        }

    }


    // TODO: получил приветствие, поставил канал в write и метку GREETING,
    // дальше нужно будет ответить ему ServerChoice
    private void readGreeting(SelectionKey key, SocketChannel channel, Message message) throws IOException {
        channel.read(message.getIn());
        byte[] msg = message.getIn().array();

        if ((msg[0] != SocksConsts.VER) || (msg.length < 3)) {
            //ошибка подключения
            close(key);
        }
        byte[] authMethods = Arrays.copyOfRange(msg, 2, 2 + msg[1]);
        boolean isAuthed = false;

        for (byte authMethod : authMethods) {
            if (authMethod == SocksConsts.NO_AUTH) {
                System.out.println("Got greeting: " + channel.socket());
                message.setStep(Message.Step.GREETING);
                key.interestOps(SelectionKey.OP_WRITE);
                key.attach(message);
                isAuthed = true;
                break;
            }
        }
        if (!isAuthed) {
            message.setStep(Message.Step.ERROR);
            key.interestOps(SelectionKey.OP_WRITE);
            key.attach(message);
        }
    }

    private void readHeader(SelectionKey key, Message message) {
        byte[] msg = message.getIn().array();


    }

    private void close(SelectionKey key) {
    }

}