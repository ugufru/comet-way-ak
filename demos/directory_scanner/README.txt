The DirectoryScanner agent scans a specific directory once every 15 seconds
and reports files that are either new or changed. This type of functionality
is very useful for processing files uploaded to a fileserver, or as a watchdog
to ensure important system files are not being changed by a malicious attacker.

This particular example recognizes new or changed files by comparing it's
modification date against the time the directory was last checked.


Start the DirectoryScanner agent using the command:
   ak

Open another console and change the date of this file using the command:

   touch README.txt

   (Windows: Use Notepad to change and re-save the file.)

If you change this file while the DirectoryScanner agent is running
the agent will detect and report the change.


To see how it detects the presence of new files, try this:

   cp README.txt README2.txt

   (Windows: copy README.txt README2.txt)


It would be very practical to use an agent like this for:

   * document management servers
   * system file monitoring
   * watch your inbox for new messages
   * automatically compile changed .java files

