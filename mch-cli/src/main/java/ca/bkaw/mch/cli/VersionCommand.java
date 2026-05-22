package ca.bkaw.mch.cli;


import ca.bkaw.mch.MchVersion;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.util.concurrent.Callable;

@Command(name = "version")
public class VersionCommand implements Callable<Integer> {
	@Override
	public Integer call() throws Exception {
		System.out.println("mch version is UNKNOWN with mch version number " + MchVersion.VERSION_NUMBER);
		return ExitCode.OK;
	}
}
