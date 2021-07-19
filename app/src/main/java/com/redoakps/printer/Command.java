package com.redoakps.printer;

public class Command {
    private byte[] mCommand;
    private boolean mIsBitmapCommand;
    private boolean mIsEndOfPrintJob;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Two initializers for adding a command or adding a end of print job notifier
    public Command(byte[] command, boolean isBitmapCommand) {
        mCommand = command;
        mIsBitmapCommand = isBitmapCommand;
        mIsEndOfPrintJob = false;
    }

    public Command() {
        mCommand = null;
        mIsBitmapCommand = false;
        mIsEndOfPrintJob = true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public byte[] getCommand() {
        return mCommand;
    }

    public boolean isBitmapCommand() {
        return mIsBitmapCommand;
    }

    public boolean isEndOfPrintJob() {
        return mIsEndOfPrintJob;
    }
}
