/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.trace;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to define "concepts" present in the Linux kernel (represented by
 * its tracepoints), that can then be exposed by different tracers under
 * different names.
 *
 * @author Alexandre Montplaisir
 * @author Matthew Khouzam - Javadoc
 */
public interface IKernelAnalysisEventLayout {

    // ------------------------------------------------------------------------
    // Common definitions
    // ------------------------------------------------------------------------

    /**
     * The standard layout, very useful for test vectors that are not kernel
     * based.
     */
    IKernelAnalysisEventLayout DEFAULT_LAYOUT = DefaultEventLayout.INSTANCE;

    /**
     * Whenever a process appears for the first time in a trace, we assume it
     * starts inside this system call. (The syscall prefix is defined by the
     * implementer of this interface.)
     *
     * TODO Change to a default method with Java 8?
     */
    String INITIAL_SYSCALL_NAME = "clone"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /**
     * The system has just entered an interrupt handler or interrupt service
     * routine. On some systems, this is known as the first level interrupt
     * handler.
     *
     * @return the event name
     */
    String eventIrqHandlerEntry();

    /**
     * The system will soon return from an interrupt handler or interrupt
     * service routine.
     *
     * @return the event name
     */
    String eventIrqHandlerExit();

    /**
     * Whenever a system call is about to return to userspace, or a hardware
     * interrupt handler exits, any 'software interrupts' which are marked
     * pending (usually by hardware interrupts) are run. Much of the real
     * interrupt handling work is done here. The soft IRQ is also known as a
     * deferred IRQ in windows. An event identifying as this needs to occur as
     * the system is beginning to process the interrupt.
     *
     * @return the event name
     */
    String eventSoftIrqEntry();

    /**
     * Whenever a system call is about to return to userspace, or a hardware
     * interrupt handler exits, any 'software interrupts' which are marked
     * pending (usually by hardware interrupts) are run Much of the real
     * interrupt handling work is done here. The soft IRQ is also known as a
     * deferred IRQ in windows. An event identifying as this needs to occur as
     * the system is returning from the interrupt.
     *
     * @return the event name
     */
    String eventSoftIrqExit();

    /**
     * Whenever a system call is about to return to userspace, or a hardware
     * interrupt handler exits, any 'software interrupts' which are marked
     * pending (usually by hardware interrupts) are run Much of the real
     * interrupt handling work is done here. The soft IRQ is also known as a
     * deferred IRQ in windows. An event identifying as this needs to occur as
     * the system is signaling the need to enter the interrupt.
     *
     * @return the event name
     */
    String eventSoftIrqRaise();

    /**
     * The scheduler will call a scheduler switch event when it is removing a
     * task from a cpu and placing another one in its place. Which task and when
     * depend on the scheduling strategy and the task priorities. This is a
     * context switch.
     *
     * @return the event name
     */
    String eventSchedSwitch();

    /**
     * sched_PI_setprio is a tracepoint called often when the schedulder
     * priorities for a given task changes.
     *
     * @return the event name
     * @since 1.0
     */
    String eventSchedPiSetprio();

    /**
     * Scheduler is waking up a task. this happens before it is executed, and
     * the data is loaded in memory if needed.
     *
     * @return the event names, as there are often several different ways to
     *         wake up
     */
    Collection<String> eventsSchedWakeup();

    /**
     * Scheduler just forked a process, that means it has duplicated the program
     * and assigned it a different process ID. This event is often followed by
     * an {@link #eventSchedProcessExec()}. In windows, this is part of the
     * "spawn" process.
     *
     * @return the event name
     */
    String eventSchedProcessFork();

    /**
     * The process has finished running and the scheduler takes its TID back.
     *
     * @return the event name
     */
    String eventSchedProcessExit();

    /**
     * The process free tracepoint is called when a process has finished running
     * and the scheduler retrieves it's process ID.
     *
     * @return the event name
     */
    String eventSchedProcessFree();

    /**
     * Optional event used by some tracers to deliver an initial state.
     *
     * @return the event name
     */
    @Nullable String eventStatedumpProcessState();

    /**
     * System call entry prefix, something like "sys_open" or just "sys".
     *
     * @return the event name
     */
    String eventSyscallEntryPrefix();

    /**
     * System call compatibility layer entry prefix, something like
     * "compat_sys".
     *
     * @return the event name
     */
    String eventCompatSyscallEntryPrefix();

    /**
     * System call exit prefix, something like "sys_exit".
     *
     * @return the event name
     */
    String eventSyscallExitPrefix();

    /**
     * The scheduler replaced the current process image with a new one. The
     * process should also be renamed at this point. In windows, this is part of
     * the spawn process as well as fork.
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventSchedProcessExec();

    /**
     * The scheduler calls wakeup on a sleeping process. The process will
     * probably soon be scheduled in.
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventSchedProcessWakeup();

    /**
     * The scheduler calls wakeup on a sleeping process. The process will
     * probably soon be scheduled in. The new wakeup knows who triggered the
     * wakeup.
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventSchedProcessWakeupNew();


    /**
     * Starting the high resolution timer
     * <p>
     * In Linux, High resolution timers are used in the following:
     * <ul>
     * <li>nanosleep</li>
     * <li>itimers</li>
     * <li>posix timers</li>
     * </ul>
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventHRTimerStart();

    /**
     * Canceling the high resolution timer
     * <p>
     * In Linux, High resolution timers are used in the following:
     * <ul>
     * <li>nanosleep</li>
     * <li>itimers</li>
     * <li>posix timers</li>
     * </ul>
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventHRTimerCancel();

    /**
     * Entering the high resolution timer expired handler.
     * <p>
     * In Linux, High resolution timers are used in the following:
     * <ul>
     * <li>nanosleep</li>
     * <li>itimers</li>
     * <li>posix timers</li>
     * </ul>
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventHRTimerExpireEntry();

    /**
     * Exiting the high resolution timer expired handler.
     * <p>
     * In Linux, High resolution timers are used in the following:
     * <ul>
     * <li>nanosleep</li>
     * <li>itimers</li>
     * <li>posix timers</li>
     * </ul>
     *
     * @return the event name
     *
     * @since 2.0
     */
    String eventHRTimerExpireExit();

    // ------------------------------------------------------------------------
    // Event field names
    // ------------------------------------------------------------------------

    /**
     * The field with the IRQ number. This is used in irq_handlers (entry and
     * exit). For soft IRQs see {@link #fieldVec}.
     *
     * @return the name of the field with the IRQ number
     */
    String fieldIrq();

    /**
     * The field with the vector. This is the soft IRQ vector field used in soft
     * IRQ raise, entry and exit. For hardware IRQs see {@link #fieldIrq}.
     *
     * @return the name of the field with the soft IRQ vector name
     */
    String fieldVec();

    /**
     * The field with the thread ID. This is often used in scheduler calls to
     * know which thread is being affected. (normally not in switch, but in
     * priority and wakeup calls).
     *
     * @return the name of the field with the thread ID
     */
    String fieldTid();

    /**
     * The field with the previous thread id. This is used in switching
     * operations of a scheduler, when a thread is scheduled out for another,
     * this field shows the thread id being scheduled out.
     *
     * @return The name of the field with the ID of the previous thread
     */
    String fieldPrevTid();

    /**
     * The field with the state of the previous thread. This is used in
     * switching operations of a scheduler, when a thread is scheduled out for
     * another, this field shows the state of the thread being scheduled out.
     *
     * @return the name of the field of the previous thread's state
     */
    String fieldPrevState();

    /**
     * The field with the next command to be run. This is used in switching
     * operations of a scheduler, when a thread is scheduled out for another,
     * this field shows the command being scheduled in. A command's value is
     * often a String like "ls" or "hl3.exe".
     *
     * @return the name of the field with the next command to be run
     */
    String fieldNextComm();

    /**
     * The field with the next thread ID. This is used in switching operations
     * of a scheduler, when a thread is scheduled out for another, this field
     * shows the thread being scheduled in.
     *
     * @return the name of the field with the next thread ID
     */
    String fieldNextTid();

    /**
     * The field with the child command. This field is used in clone and spawn
     * activities, to know which executable the clone is running.
     *
     * @return the name of the field with the child command
     */
    String fieldChildComm();

    /**
     * The field with the parent thread ID. This field is used in clone and
     * spawn activities, to know which thread triggered the clone.
     *
     * @return the name of the field with the parent thread ID
     */
    String fieldParentTid();

    /**
     * The field with the child thread ID. This field is used in clone and spawn
     * activities, to know which thread is the clone.
     *
     * @return the name of the field with the child thread ID
     */
    String fieldChildTid();

    /**
     * The field with the command. This is used in scheduling tracepoints that
     * are not switches, and show the current process name. It is often a string
     * like "zsh" or "cmd.exe".
     *
     * @return the name of the command field
     * @since 2.0
     */
    String fieldComm();

    /**
     * The field with the name. The name field is used in several disjoint
     * events.
     * <p>
     * Examples include:
     * <ul>
     * <li>writeback_* - the name of the io device, often "(unknown)"</li>
     * <li>module_* - the name of the module such as "binfmt_misc"</li>
     * <li>irq_handler_entry - the field describes the name of the handler such
     * as "i915"</li>
     * <ul>
     *
     * @return the name of the field with a name
     * @since 2.0
     */
    String fieldName();

    /**
     * The field with the status. Often functions like a return value before we
     * hit an exit.
     * <p>
     * Examples include:
     * <ul>
     * <li>ext4* - status</li>
     * <li>asoc_snd_soc_cache_sync</li>
     * <li>rpc_*</li>
     * <li>state dumps</li>
     * </ul>
     *
     * @return The name of the field with a status
     * @since 2.0
     */
    String fieldStatus();

    /**
     * The field with the last command to be run. This is often a string
     * representing the command of the thread being scheduled out from a
     * scheduler switch operation.
     *
     * @return the name of the field with the last command to be run
     * @since 2.0
     */
    String fieldPrevComm();

    /**
     * The field with the file name field. This is a string used mostly with
     * file operations. These operations are often wrapped in system calls and
     * can be:
     * <ul>
     * <li>open</li>
     * <li>change mode</li>
     * <li>change directory</li>
     * <li>stat</li>
     * </ul>
     * It can also be used in exec commands to see what the command name should
     * be.
     * <p>
     * Please note that file read and write often do not use the file name, they
     * just use the file handle.
     *
     * @return the name of the field with the file name
     * @since 2.0
     */
    String fieldFilename();

    /**
     * The field with the priority. The priority of a given process is used by
     * most scheduler events. The major exception is the switching operation as
     * it has two processes so it has a previous and next priority.
     *
     * @return the name of the field with the thread or process' priority
     * @since 1.0
     */
    String fieldPrio();

    /**
     * The field with the new priority. This is used in the scheduler's
     * pi_setprio event event to show the new priority of the thread or process.
     *
     * @return the name of the field with the thread or process' new priority
     * @since 1.0
     */
    String fieldNewPrio();

    /**
     * The field with the next priority. This is used in the scheduler's switch
     * event to show the priority of the next thread or process.
     *
     * @return the name of the field with the thread or process' next priority
     * @since 1.0
     */
    String fieldNextPrio();

    /**
     * The field with the hrtimer. The hrtimer holds the timer instance.
     *
     * @return the name of the hrTimer field
     * @since 2.0
     */
    String fieldHRtimer();

    /**
     * The field with the expires value. The expires field holds the expiry time.
     * of the hrtimer.
     *
     * @return the name of the expires field
     * @since 2.0
     */
    String fieldHRtimerExpires();

    /**
     * Gets the field name with the softexpires value. The softexpire value is the
     * absolute earliest expiry time of the hrtimer.
     *
     * @return the name of the softexpires field
     * @since 2.0
     */
    String fieldHRtimerSoftexpires();

    /**
     * The field of the function address value. The function field holds timer
     * expiry callback function.
     *
     * @return the name of the function field
     * @since 2.0
     */
    String fieldHRtimerFunction();

    /**
     * The field of the now value. The now field holds the current time.
     *
     * @return the name of the now field (hrtimer)
     * @since 2.0
     */
    String fieldHRtimerNow();

}
