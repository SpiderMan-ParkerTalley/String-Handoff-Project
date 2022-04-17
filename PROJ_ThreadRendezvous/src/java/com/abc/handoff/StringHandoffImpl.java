package com.abc.handoff;

import java.util.*;

import com.abc.pp.stringhandoff.*;
import com.programix.thread.*;

/**
* StringHandoffImpl acts as a message queue for passing and receiving cross-thread-messages.
*/
public class StringHandoffImpl implements StringHandoff {
    
    /**
     * Current message waiting to be passed.
     */
    private volatile LinkedList<String> queue = new LinkedList<>();
    
    /**
     * States whether the system should shutdown or not.
     */
    private volatile boolean shutdown = false;
    
    /**
     * Message sent
     */
    private volatile boolean messageAlreadyInQueue = false;
    
    /**
     * Receiver in queue.
     */
    private volatile boolean receiverAlreadyInQueue = false;

    /**
     * Attempts to hand off the specified String to a receiver and waits inside until the hand-off can be completed
     * (or time runs out).
     *
     * @param msg the String to be handed off to a receiver
     * @param msTimeout number of milliseconds to wait for the hand-off to occur or zero to wait without ever timing
     * out. If msTimeout is negative, it should be treated as a very short timeout.
     * For a full explanation of the exceptions, see the interface Javadoc.
     * @throws InterruptedException if the calling thread is interrupted
     * @throws TimedOutException if time runs out AND the hand-off has not yet occurred
     * @throws ShutdownException if <tt>StringHandoff</tt> has been shutdown
     * @throws IllegalStateException if there is already a thread waiting to pass
     */
    @Override
    public synchronized void pass(String msg, long msTimeout)
        throws InterruptedException, TimedOutException, ShutdownException, IllegalStateException {
        
        try {
            // Check if the current thread has been interrupted.
            if(Thread.currentThread().isInterrupted() == true) {
                // Set the message already in queue status to false.
                messageAlreadyInQueue = false;
                
                // Set shutdown status to false.
                shutdown = false;
            }
           
            // Check if a shutdown request has been submitted.
            checkForShutdown();
             
            // Check if message has already been set.
            if (messageAlreadyInQueue) {
                throw new IllegalStateException();
            }
            
            // Negative msTimeout passed check.
            if (msTimeout < 0) {
                msTimeout = 100L;
            }
           
            // Convert null to null string format.
            if(msg == null) {
                msg = "null";
            }
            
            // Set the message.
            queue.add(msg);
            
            // Message received.
            messageAlreadyInQueue = true;
            
            // Notify all threads.
            notifyAll();
            
            // Test case: multiple items pass and receive [0ms timeout: never times out]
            if (msTimeout == 0L) {
                while (queue.size() != 0) {
                    // Wait.
                    wait();
                    
                    // If a shut down request has been submitted, throw shutdown exception.
                    checkForShutdown();
                }
            }
            
            // else: msTimeout is not 0.
            else {
             // Calculate the current wait time with added system time.
                long timeLimit = System.currentTimeMillis() + msTimeout;
                
                // If the current message is not null...
                while(queue.size() != 0) {
                    // Calculate the time waited.
                    long waitTime = timeLimit - System.currentTimeMillis();
                    
                    // If the time waited exceeds the limit given, throw timed out exception.
                    if (waitTime <= 0) {
                        
                        messageAlreadyInQueue = false;
                        
                        // Remove the last added item.
                        queue.removeLast();
                        
                        // If a shut down request has been submitted, throw shutdown exception.
                        throw new TimedOutException();
                    }
                    
                    // Wait the remaining time.
                    wait(waitTime);
                    
                    // If a shut down request has been submitted, throw shutdown exception.
                    checkForShutdown();
                }
            }
            
            // Set the message already in queue status to false.
            messageAlreadyInQueue = false;
        } 
        // Catch the interrupted exception to reset appropriate fields.
        catch (InterruptedException e){
            // Set the message already in queue status to false.
            messageAlreadyInQueue = false;
            
            // Set shutdown status to false.
            shutdown = false;
            
            // Remove the last added item.
            queue.removeLast();
            
            // Throw the exception.
            throw e;
        }
    }

    
    /**
     * Attempts to hand off the specified String to a receiver and waits inside until the hand-off can be completed.
     *
     * @param msg the String to be handed off to a receiver
     * For a full explanation of the exceptions, see the interface Javadoc.
     * @throws InterruptedException if the calling thread is interrupted
     * @throws ShutdownException if <tt>StringHandoff</tt> has been shutdown
     * @throws IllegalStateException if there is already a thread waiting to pass
     */
    @Override
    public synchronized void pass(String msg) throws InterruptedException, ShutdownException, IllegalStateException {
        // Pass to main pass method. 
        pass(msg, 0L);
    }

    
    /**
     * Attempts to receive a String handed off by a passer and waits inside until the hand-off can be completed
     * (or time runs out).
     *
     * @param msTimeout number of milliseconds to wait for the hand-off to occur or zero to wait without ever timing
     * out. If msTimeout is negative, it should be treated as a very short timeout.
     * For a full explanation of the exceptions, see the interface Javadoc.
     * @throws InterruptedException if the calling thread is interrupted
     * @throws TimedOutException if time runs out AND the hand-off has not yet occurred
     * @throws ShutdownException if <tt>StringHandoff</tt> has been shutdown
     * @throws IllegalStateException if there is already a thread waiting to receive
     */
    @Override
    public synchronized String receive(long msTimeout)
        throws InterruptedException, TimedOutException, ShutdownException, IllegalStateException {
            
        try {
            // Check if the current thread has been interrupted.
            if(Thread.currentThread().isInterrupted() == true) {
                // Set the receiver already in queue status to false.
                receiverAlreadyInQueue = false;
                
                // Set shutdown status to false.
                shutdown = false;
            }
            
            // Check if a shutdown request has been submitted.
            checkForShutdown();
    
            // Check if the system already has a receiver waiting.
            if (receiverAlreadyInQueue) {
                throw new IllegalStateException();
            }
            
    
            // Set the receiver already in queue status to true.
            receiverAlreadyInQueue = true;
            
            // Notify all threads that receiverAlreadyInQueue has been changed.
            notifyAll();
            
            // Negative msTimeout passed check.
            if (msTimeout < 0) {
                msTimeout = 100L;
            }
            
            // Test case: multiple items pass and receive [0ms timeout: never times out]
            if (msTimeout == 0L) {
                while (queue.isEmpty() == true) {
                    // Wait.
                    wait();
                    
                    // If a shut down request has been submitted, throw shutdown exception.
                    checkForShutdown();
                }
            }
            
            // else: msTimeout is not 0.
            else {
                // Calculate the current wait time with added system time.
                long timeLimit = System.currentTimeMillis() + msTimeout;
                
                // While the message is null, wait.
                while(queue.isEmpty() == true) {
                    
                    // Calculate the time waited.
                    long waitTime = timeLimit - System.currentTimeMillis();
                    
                    // If the time waited exceeds the limit given, throw timed out exception.
                    if (waitTime <= 0) {       
                        // Set the receiver already in queue status to false.
                        receiverAlreadyInQueue = false;
                        
                        // Throw new timed out exception; time limit exceeded.
                        throw new TimedOutException();
                    }
                    
                    // Wait the remaining time.
                    wait(waitTime);
    
                    // If a shut down request has been submitted, throw shutdown exception.
                    checkForShutdown();
                }
            }
            
            // Set the receiver already in queue status to false.
            receiverAlreadyInQueue = false;
    
            // Remove the first added string for the queue.
            String returnMessage = queue.remove();
            
            // Convert null string to actual null.
            if (returnMessage == "null") {
                returnMessage = null;
            }
            
            // Notify all threads.
            notifyAll();
            
            // Return the message to the calling thread.
            return returnMessage;
        } 
        // Catch the interrupted exception to reset appropriate fields.
        catch (InterruptedException e) {
            // Set the receiver already in queue status to false.
            receiverAlreadyInQueue = false;
            
            // Set shutdown status to false.
            shutdown = false;
            
            // Throw the exception.
            throw e;
        }
    }

    
    /**
     * Attempts to receive a String handed off by a passer and waits inside until the hand-off can be completed.
     *
     * For a full explanation of the exceptions, see the interface Javadoc.
     * @throws InterruptedException if the calling thread is interrupted
     * @throws ShutdownException if <tt>StringHandoff</tt> has been shutdown
     * @throws IllegalStateException if there is already a thread waiting to receive
     */
    @Override
    public synchronized String receive() throws InterruptedException, ShutdownException, IllegalStateException {
        // Pass to main receiver method.
        return receive(0L);
    }
    
    /**
     * Called to shutdown this instance of <tt>StringHandoff</tt>.
     * This call is asynchronous (does not wait, but simply signal that a shutdown has been requested).
     * This method may be safely called any number of times.
     * This method never throws any exceptions.
     */
    @Override 
    public synchronized void shutdown() {
        // Set the shutdown status to true.
        shutdown = true;
        // Empty the queue to allow garbage collection.
        queue.clear();
        // Set the receiver already in queue status to false.
        receiverAlreadyInQueue = false;
        // Set the message already in queue status to false.
        messageAlreadyInQueue = false;
        // Notify all threads.
        notifyAll();
    }

    /**
     * Return a reference to the object being used for synchronization.
     * This can be used to allow the caller to bridge holding the lock between method calls.
     */
    @Override
    public Object getLockObject() {
        return this;
    }
    
    /**
     * Check if the system is in shutdown state.
     */
    private void checkForShutdown() {
        // Check the shutdown status.
        if (shutdown) {
            // Throw shutdown exception.
            throw new ShutdownException();
        }
    }
}