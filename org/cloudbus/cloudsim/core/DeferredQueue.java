/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class implements the deferred event queue used by {@link Simulation}. The event queue uses a
 * linked list to store the events.
 * 
 * @author Marcos Dias de Assuncao
 * @since CloudSim Toolkit 1.0
 * @see Simulation
 * @see SimEvent
 */
public class DeferredQueue {

	/** The list. */
	private final List<SimEvent> list = new LinkedList<SimEvent>();

	/** The max time. */
	private double maxTime = -1;

	/**
	 * Adds a new event to the queue. Adding a new event to the queue preserves the temporal order
	 * of the events.
	 * 
	 * @param newEvent The event to be added to the queue.
	 */
	public void addEvent(SimEvent newEvent) {
		// The event has to be inserted as the last of all events
		// with the same event_time(). Yes, this matters.
		double eventTime = newEvent.eventTime();
		//如果新加入的事件，时间比较大，就加入到队列的最后。
		if (eventTime >= maxTime) {
			list.add(newEvent);
			maxTime = eventTime;
			return;
		}

		/**
		 * 否则，按照事件的时间顺序，对事件进行排序，将事件加入
		 * 到适当的位置。
		 * 
		 * iterator.add(newEvent);在当前迭代的后面插入新的节点。
		 * 
		 * 最后的结果： 队列中的事件按照时间先后顺序排序。
		 */
		ListIterator<SimEvent> iterator = list.listIterator();
		SimEvent event;
		while (iterator.hasNext()) {
			event = iterator.next();
			if (event.eventTime() > eventTime) {
				iterator.previous();
				iterator.add(newEvent);
				return;
			}
		}

		list.add(newEvent);
	}

	/**
	 * Returns an iterator to the events in the queue.
	 * 
	 * @return the iterator
	 */
	public Iterator<SimEvent> iterator() {
		return list.iterator();
	}

	/**
	 * Returns the size of this event queue.
	 * 
	 * @return the number of events in the queue.
	 */
	public int size() {
		return list.size();
	}

	/**
	 * Clears the queue.
	 */
	public void clear() {
		list.clear();
	}

}