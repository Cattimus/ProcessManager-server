import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

//TODO - enable/disable task (without deleting)
//TODO - elapse function (possibly implemented in ManagedProcess)

public class ScheduledTask {
	private LocalDateTime elapseTime; //the local time the task is set to activate next
	private LocalTime timeOfDay;      //the time of day the task is set to activate
	private String signal;            //this signal will be sent to the process upon elapse
	private Duration frequency;       //upon elapse, the new time will be set to localtime.now() + duration

	private final boolean sendStop;
	private final boolean sendStart;
	private final boolean sendSignal;
	private final boolean sendRestart;

	ScheduledTask(Builder toCopy) {
		signal      = toCopy.signal;
		sendStop    = toCopy.sendStop;
		frequency   = toCopy.frequency;
		sendStart   = toCopy.sendStart;
		sendSignal  = toCopy.sendSignal;
		elapseTime  = toCopy.elapseTime;
		sendRestart = toCopy.sendRestart;
	}

	public void newSignal(String updatedSignal) {
		signal = updatedSignal;
	}

	public String getSignal() {
		return signal;
	}

	public void changeTime(LocalTime scheduleTime) {
		timeOfDay = scheduleTime;
		elapseTime = LocalDate.now().atTime(scheduleTime);
	}

	public void changeFrequency(Duration newFrequency) {
		frequency = newFrequency;
	}

	public boolean isElapsed() {
		if(elapseTime.isBefore(LocalDateTime.now())) {
			return true;
		}
		return false;
	}

	public void reset() {
		elapseTime = elapseTime.plus(frequency);
		System.out.println("New timer has been set for: " + elapseTime);
	}

	public static class Builder {
		private LocalDateTime elapseTime = null;
		private LocalTime timeOfDay = null;
		private String signal = null;
		private Duration frequency = null;

		private boolean sendStop    = false;
		private boolean sendStart   = false;
		private boolean sendRestart = false;
		private boolean sendSignal  = false;

		private Builder() {
			elapseTime = LocalDateTime.now(); //by default elapse time is set to the instant it's created
			timeOfDay = LocalTime.now();      //by default elapse time is set to the instant it's created
			frequency = Duration.ofDays(1);   //by default frequency is set to daily
		}

		public static Builder newInstance() {
			return new Builder();
		}

		private void clearFlags() {
			sendStop = false;
			sendStart = false;
			sendRestart = false;
			sendSignal = false;
		}

		//assign an elapse time
		public Builder at(LocalDateTime elapse) {
			this.elapseTime = elapse;
			return this;
		}

		//assign a signal to be sent upon elapse
		public Builder sendSignal(String signal) {
			this.clearFlags();
			this.sendSignal = true;
			this.signal = signal;
			return this;
		}

		//unsafe (non-signal) stop at elapse
		public Builder unsafeStop() {
			this.clearFlags();
			this.sendStop = true;
			return this;
		}

		//start process upon elapse
		public Builder startProcess() {
			this.clearFlags();
			this.sendStart = true;
			return this;
		}

		//restart process upon relapse
		public Builder restartProcess() {
			this.clearFlags();
			this.sendRestart = true;
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

		//local time of day to expire (any frequency)
		public Builder at(LocalTime scheduledTime) {
			this.timeOfDay = scheduledTime;
			this.elapseTime = LocalDate.now().atTime(scheduledTime);
			return this;
		}

		//return built object
		public ScheduledTask build() {
			return new ScheduledTask(this);
		}

	}
}
