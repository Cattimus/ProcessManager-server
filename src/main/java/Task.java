import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.json.*;

public class Task {
	public enum SignalType{START, STOP, RESTART, SIGNAL, NONE}
	private LocalDateTime elapseTime; //the local time the task is set to activate next
	private String signal;            //this signal will be sent to the process upon elapse
	private String taskName;		  //identify the task for the log
	private Duration frequency;       //upon elapse, the new time will be set to localtime.now() + duration
	private SignalType type;		  //signals which action to take by the scheduling thread
	private boolean enabled;		  //signals if task is active or inactive (inactive tasks are automatically removed)
	private boolean oneTime;		  //signals if the task should reactivate itself upon reset()

	Task(Builder toCopy) {
		type 		= toCopy.type;
		signal      = toCopy.signal;
		enabled     = toCopy.enabled;
		oneTime 	= toCopy.oneTime;
		taskName  	= toCopy.taskName;
		frequency   = toCopy.frequency;
		elapseTime  = toCopy.elapseTime;
	}

	//deserialize from JSON object
	Task(JSONObject data) {
		type = stringToType(data.getString("type"));
		taskName = data.getString("name");
		elapseTime = LocalDateTime.parse(data.getString("elapse-time"));
		frequency = Duration.parse(data.getString("frequency"));
		enabled = data.getBoolean("enabled");
		oneTime = data.getBoolean("one-time");

		//extract signal field
		var signalTemp = data.get("signal");
		if(signalTemp != JSONObject.NULL) {
			signal = signalTemp.toString();
		} else {
			signal = null;
		}

		//set local time in the future if it's in the past
		if(elapseTime.isBefore(LocalDateTime.now())) {
			LocalTime temp = elapseTime.toLocalTime();
			elapseTime = LocalDateTime.now();
			setElapseTime(temp);
			elapseTime = elapseTime.plus(frequency);
		}
	}

	private String typeToString(SignalType data) {
		switch(data) {
			case START:
				return "start";

			case STOP:
				return "stop";

			case RESTART:
				return "restart";

			case SIGNAL:
				return "signal";

			default:
				return "none";
		}
	}

	private SignalType stringToType(String data) {
		switch(data) {
			case "start":
				return SignalType.START;

			case "stop":
				return SignalType.STOP;

			case "restart":
				return SignalType.RESTART;

			case "signal":
				return SignalType.SIGNAL;

			default:
				return SignalType.NONE;
		}
	}

	//change local time
	public void setElapseTime(LocalTime scheduleTime) {
		Duration toAdjust = Duration.between(elapseTime.toLocalTime(), scheduleTime);
		if(elapseTime.plus(toAdjust).isBefore(LocalDateTime.now())) {
			//add a day if the time is in the past
			toAdjust = toAdjust.plusDays(1);
		}
		elapseTime = elapseTime.plus(toAdjust);
	}

	//change date and time
	public void setElapseTime(LocalDateTime scheduleDateTime) {
		//don't set time if it's in the past
		if(LocalDateTime.now().isAfter(scheduleDateTime)) {
			return;
		}
		elapseTime = scheduleDateTime;
	}

	//reset function (move forward 1 frequency). if task is one-time it is disabled
	public void reset() {
		if(!oneTime) {
			elapseTime = elapseTime.plus(frequency);
		} else {
			enabled = false;
		}
	}

	/* FORMAT
	   type: task
	   name: taskName
	   signal: signal
	   elapse-time: standardized elapse time
	   frequency: standardized frequency
	   enabled: enabled(boolean)
	   one-time: oneTime(boolean)
	 */
	public JSONObject toJSON() {
		JSONObject record = new JSONObject();
		record.put("ID", "task");
		record.put("type", typeToString(this.type));
		record.put("name", taskName);
		record.put("elapse-time", elapseTime.toString());
		record.put("frequency", frequency.toString());
		record.put("enabled", enabled);
		record.put("one-time", oneTime);

		if(signal != null) {
			record.put("signal", signal);
		} else {
			record.put("signal", JSONObject.NULL);
		}

		return record;
	}

	public String toString() {
		String toReturn = "";

		toReturn += "Task name: " 	 + taskName + "\n";
		toReturn += "Elapse time: "  + elapseTime + "\n";
		toReturn += "Type: " 		 + typeToString(type) + "\n";
		toReturn += "Signal: " 		 + signal + "\n";
		toReturn += "Frequency: "    + frequency + "\n";
		toReturn += "Enabled: " 	 + enabled + "\n";
		toReturn += "One time: " 	 + oneTime;

		return toReturn;
	}

	//setter/getter for enabled
	public void enable() {
		enabled = true;
	}
	public void disable() {
		enabled = false;
	}
	public boolean isEnabled() {
		return enabled;
	}

	//time functions
	public LocalDateTime getElapseTime() {
		return elapseTime;
	}
	public boolean isElapsed() {
		return elapseTime.isBefore(LocalDateTime.now());
	}

	//signal functions
	public void newSignal(String updatedSignal) {
		signal = updatedSignal;
	}
	public String getSignal() {
		return signal;
	}

	//general getters
	public String getName() {
		return taskName;
	}
	public SignalType getType() {
		return type;
	}
	public void changeFrequency(Duration newFrequency) {
		frequency = newFrequency;
	}

	public static class Builder {
		private LocalDateTime elapseTime;
		private String signal = null;
		private String taskName = null;
		private Duration frequency;
		private SignalType type;
		private boolean enabled = true;
		private boolean oneTime = false;

		private Builder(String name) {
			this.elapseTime = LocalDateTime.now(); //by default elapse time is set to the instant it's created
			this.frequency = Duration.ofDays(1);   //by default frequency is set to daily
			this.type = SignalType.NONE;
			this.taskName = name;
		}

		public static Builder newInstance(String name) {
			return new Builder(name);
		}

		//assign a signal to be sent upon elapse
		public Builder sendSignal(String signal) {
			this.type = SignalType.SIGNAL;
			this.signal = signal;
			return this;
		}

		//unsafe (non-signal) stop at elapse
		public Builder unsafeStop() {
			this.type = SignalType.STOP;
			return this;
		}

		//start process upon elapse
		public Builder startProcess() {
			this.type = SignalType.START;
			return this;
		}

		//restart process upon relapse
		public Builder restartProcess() {
			this.type = SignalType.RESTART;
			return this;
		}

		//one time frequency
		public Builder once() {
			this.oneTime = true;
			return this;
		}

		//daily frequency
		public Builder daily() {
			this.frequency = Duration.ofDays(1);
			return this;
		}

		//weekly frequency
		public Builder weekly() {
			this.frequency = Duration.ofDays(7);
			return this;
		}

		//hourly frequency
		public Builder hourly() {
			this.frequency = Duration.ofHours(1);
			return this;
		}

		//custom frequency
		public Builder interval(Duration toWait) {
			this.frequency = toWait;
			return this;
		}

		//local date time to expire (any frequency)
		public Builder at(LocalDateTime scheduleTime) {
			//if current time is after scheduled time, a day must be added to make sure the date is in the future
			if(LocalDateTime.now().isAfter(scheduleTime)) {
				this.elapseTime = scheduleTime.plusDays(1);
			} else {
				this.elapseTime = scheduleTime;
			}
			return this;
		}

		public Builder at(LocalTime scheduleTime) {
			Duration toAdjust = Duration.between(elapseTime.toLocalTime(), scheduleTime);
			if(elapseTime.plus(toAdjust).isBefore(LocalDateTime.now())) {
				//add a day if the time is in the past
				toAdjust = toAdjust.plusDays(1);
			}
			elapseTime = elapseTime.plus(toAdjust);
			return this;
		}

		//return built object
		public Task build() {
			return new Task(this);
		}

	}
}
