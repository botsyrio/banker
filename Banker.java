//David A. Foley
//OS, Prof. Gottlieb
//Lab 3
//11.14.17
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Banker {
	public static void main(String[] args){
		String path = System.getProperty("user.dir");//build path to input document from cwd and args[0]
		if(path.contains("\\"))//case where system separates directories with token "//"
			path+="\\";
		else
			path+="/";//case where system separates directories with token "/"
		path+=args[0];
		
		File input = new File (path);//get file and attempt to scan it.
		Scanner yaBoi;//read the inputs
		try {
			yaBoi = new Scanner (input);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file "+args[0]+" not found at "+path);
			e.printStackTrace();
			return;
		}
		
		int numTasks = yaBoi.nextInt();
		int numResources = yaBoi.nextInt();
		
		int resources[] = new int[numResources+1];//holds the number of each resource available to the system
		for(int i = 1; i<numResources+1; i++){
			resources[i] = yaBoi.nextInt();
		}
		
		ArrayList<ArrayList<String>> commands = new ArrayList<ArrayList<String>>();//each index i holds the list of unexecuted commands for program i
		for(int i = 0; i<numTasks+1; i++)
			commands.add(new ArrayList<String>());
		
		yaBoi.nextLine();
		while(yaBoi.hasNextLine()){
			String c = yaBoi.nextLine();
			if(!c.equals("")){
			String tmp[] = c.split("\\s+");
			commands.get(Integer.valueOf(tmp[1])).add(c);
			}
		}
		yaBoi.close();

		fifo(commands, resources);
		System.out.println();
		bankers(commands, resources);
	}
	
	public static void bankers(ArrayList<ArrayList<String>>comm, int[] resources){
		ArrayList<ArrayList<String>>commands=new ArrayList<ArrayList<String>>();//full copy of the original command list
		for(int i =0; i<comm.size(); i++){
			commands.add(new ArrayList<String>());
			for(int j=0; j<comm.get(i).size(); j++){
				commands.get(i).add(comm.get(i).get(j));
			}
		}
		
		int[]tWaiting = new int[commands.size()];//at index i, holds the amount of time job i spent waiting
		int[]tTerminated = new int[commands.size()];//at index i, holds the time at which job i terminated (or -1 if job i was aborted)
		int[][]programs=new int[commands.size()][resources.length*2+3];//at programs[i], the vector contains the following information about job i:
		//[the array of its claims][the array of resources it's currently holding][a bit indicating whether the command at commands.get(i).get(0) was executed during the most recent cycle(0 if it was, 1 if it was not)][a bit indicating whether the job has terminated(0 if it has not, 1 if it has)][a bit indicating for how many cycles(if any), the job is stuck computing]
		
		for(int i=0; i<programs.length; i++){//initialize tWaiting and the last 3 bits of each entry of programs
			tWaiting[i]=0;
			programs[i][programs[i].length-3]=0;
			programs[i][programs[i].length-2]=0;
			programs[i][programs[i].length-1]=0;
		}
		
		
		ArrayList<Integer> reqQueue = new ArrayList<Integer>();//array list which will hold all the ungranted requests in FIFO order
		int t=0;//cycle number
		boolean done = false;
		while(!done){
				
			done = true;//done is true if and only if all of the jobs have terminated
			for(int i =1; i<commands.size(); i++){
				if(programs[i][programs[i].length-2] ==0){
					done = false;
				}
			}
				
			if(!done){
				
				for(int i =1; i<commands.size(); i++){//runs through all jobs and, if its next command is a claim initialization, initializes claim
					String split[] = commands.get(i).get(0).split("\\s+");
					if(split[0].equals("initiate")){//case where a claim must be initiated
								
						for(int j = 2; j<split.length; j+=2){
							
							if(Integer.valueOf(split[j+1])>resources[Integer.valueOf(split[j])]){//if the job stakes a claim beyond what the system is capable of satisfying, immediately aborts the job
								programs[i][programs[i].length-2] = 1;//sets it to finished
								tTerminated[i] = -1;//sets flag in tTerminated to indicate abortion
							}//end abort case
							
							programs[i][Integer.valueOf(split[j])]=Integer.valueOf(split[j+1]);//sets the claim
							programs[i][Integer.valueOf(split[j])+resources.length]=0;//sets the current amount held of the resource to 0
							programs[i][programs[i].length-3]=0;//indicates the command was executed
						}//end going through claims initiated by this command
					}//end if
				}//end for
				
				for(int i=1; i<commands.size(); i++){//runs through jobs, performs procedure if job is stuck computing
					if(programs[i][programs[i].length-1]!=0)//case where program is still stuck computing
						programs[i][programs[i].length-1]--;
				}//end for
					
				for(int i = 1; i<commands.size(); i++){//finds programs which must execute a compute command next
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){//case where program is not(/is no longer) stuck computing and has not been terminated
						
						String split[] = commands.get(i).get(0).split("\\s+");//retrieve next instruction the program has to execute
							
						if(split[0].equals("compute")){//case where job i's next command is a computation
							programs[i][programs[i].length-1]=Integer.valueOf(split[2]);//sets for how many cycles the computation will occur
							commands.get(i).remove(0);//removes this command from job i's list of commands to execute
						}//end if
					}//end if
				}//end for
					
				for(int i = 1; i<commands.size(); i++){//finds programs which must execute a request command next
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){//case where program is not(/is no longer) stuck computing and has not been terminated
						String split[] = commands.get(i).get(0).split("\\s+");
						if(split[0].equals("request")){//case where next instruction is a resource request
							boolean toAdd = true;//true if this request has yet to be added to the queue, false if it is already there
								
							for(int q=0; q<reqQueue.size();q++){//checks reqQueue for i
								if(reqQueue.get(q)==i)//case where this request from job i is already in the request queue
									toAdd = false;
							}//end for
								
							if(toAdd){//case where request must be added to queue
								reqQueue.add(i);
							}//end if
								
						}//end request procedure
					}//end if
				}//end for
						
				for(int q = 0; q<reqQueue.size(); q++){
					int i=reqQueue.get(q);
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0&&programs[i][programs[i].length-1]==0){
						String split[] = commands.get(i).get(0).split("\\s+");
						if(split[0].equals("request")){//case where next instruction is a resource request
							
							int rReq = Integer.valueOf(split[2]);//resource requested
							int numReq = Integer.valueOf(split[3]);//amount of resource rReq requested
								
							if(numReq<=resources[rReq]&&!(numReq+programs[i][rReq+resources.length]>programs[i][rReq])){//case where system has enough to satisfy the request and the request does not violate the job's claim
								int [][] copyPrograms = new int[programs.length][programs[0].length];//make copy of programs[][] to be changed and passed to safe()
								for(int j = 0; j<programs.length; j++){
									for(int k = 0;k<programs[j].length; k++)
										copyPrograms[j][k] = programs[j][k];
								}
									int [] copyResources = new int[resources.length];//make copy of resources to be changed and passed to safe()
								for(int j = 0; j<resources.length; j++){
									copyResources[j]=resources[j];
								}
								
								copyResources[rReq]-=numReq;//change copyResources and copyPrograms as though the request were being granted
								copyPrograms[i][rReq+resources.length]+=numReq;
							
								if(safe(copyResources, copyPrograms, 0)){//case where safe() (aka banker's algorithm) says the request is safe
									programs[i][programs[i].length-3]=0;//indicates this request command was executed
									resources[rReq]-=numReq;//removes resources from the system
									programs[i][rReq+resources.length]+=numReq;//grants resources to the job
								}//end safe case
								
								else{//case where the request is currently unsafe
									tWaiting[i]++;
									programs[i][programs[i].length-3]=1;//indicates request could not be granted
								}//end unsafe case
								
							}//end case where request is "grantable" and legal
							
							else if(numReq+programs[i][rReq+resources.length]>programs[i][rReq]){//case where the request violates the job's claims
								
								for(int res =0; res<resources.length; res++){//return all the job's resources to the system
									resources[res]+=programs[i][res+resources.length];
									programs[i][res+resources.length]=0;
								}//end resource return
									
								programs[i][programs[i].length-2]=1;//indicates job has finished
								tTerminated[i] = -1;//error code indicates job was aborted
							}//end claim violation case
							
							else if (!(numReq<=resources[rReq])){//case where request does not violate claims but the system does not have enough to satisfy the request
								tWaiting[i]++;
								programs[i][programs[i].length-3]=1;//indicate request could not be granted
							}//end poor system case
						
						}//end resource request case
					}//end if
				}//end for (iterating through reqQueue)
						
				for(int i = 1; i<commands.size(); i++){//look for jobs that are executing a release next
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){//case where job is not computing and has not terminated
						String split[] = commands.get(i).get(0).split("\\s+");
							if(split[0].equals("release")){//release command found
								int rRel = Integer.valueOf(split[2]);//resource to be released
								int numRel = Integer.valueOf(split[3]);//quantity of resource to be released
							
								resources[rRel]+=numRel;//add resource back to resources
								programs[i][rRel+resources.length]-=numRel;
								programs[i][programs[i].length-3]=0;
						}
					}
				}
					
				for(int i = 1; i<commands.size(); i++){//find any processes about to terminate
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0&&programs[i][programs[i].length-1]==0){
						
						String split[] = commands.get(i).get(0).split("\\s+");
						if(split[0].equals("terminate")){//case where job i's next instruction is a termination
							for(int j =1; j<resources.length; j++)//return the job's resources to the system
								resources[j]+=programs[i][j+resources.length];
							
							programs[i][programs[i].length-2]=1;//set job to finished
							tTerminated[i]=t;//set time terminated
						}
					}//end if
				}//end finding processes about to terminate
				
				for(int i = 1; i<commands.size(); i++){//removes the most recent command from all jobs which were able to execute such command
					if(programs[i][programs[i].length-3]==0&&programs[i][programs[i].length-2]==0&&programs[i][programs[i].length-1]==0){
						commands.get(i).remove(0);
						for(int q=0; q<reqQueue.size(); q++){//if executed command was a request, remove the job from the request queue
							if(reqQueue.get(q)==i)
								reqQueue.remove((int)q);
						}//end for
					}//end if
				}//end finding commands which have executed their most recent command
						
			}//end case where all the jobs have not yet finished
			t++;//increment cycle
		}//end while(!done)
		
		//print all final output
		System.out.println("Bankers");
		int totalComputation = 0;//cumulative time terminated for all the jobs
		int totalWaiting = 0;//cumulative time spent waiting by all the jobs
		
		for(int i = 1; i<tTerminated.length; i++){//print output for each job
			if(tTerminated[i]!=-1){//case where job was not aborted
				totalComputation+=tTerminated[i];
				totalWaiting+=tWaiting[i];
				System.out.println("Task "+i+"\t\t"+tTerminated[i]+"\t"+tWaiting[i]+"\t"+(int)((((double)tWaiting[i])/((double)tTerminated[i]))*100)+"%");
			}
			else//case where job was aborted
				System.out.println("Task "+i+"\t\taborted");
				
		}//end for
			
		System.out.println("total\t\t"+totalComputation+"\t"+totalWaiting+"\t"+(int)((((double)totalWaiting)/((double)totalComputation))*100)+"%");//print cumulative output
	}//end bankers
	
	static boolean safe(int[] resources, int[][]programs, int numDone){//takes as input the resources of the system, the program graph, and the number of processes finished thus far
	//returns true if there is an ordering of the programs s.t. all programs will terminate. otherwise, returns false
		
		if(numDone==programs.length-1)//case where a complete legal ordering of processes has been found
			return true;
			
		for(int i=1; i<programs.length; i++){//finds out if there is a program that could legally be guaranteed to finish next
			boolean good = true;
			for(int j = 1; j<resources.length; j++){
				if(programs[i][j]-programs[i][j+resources.length]>resources[j]){//case where the system does not have enough of some resource j to satisfy all of job i's claims
					good = false;
				}
			}
			
			if(good){//case where job i is a good candidate
				
				int[][] copyPrograms = new int[programs.length][programs[0].length];//makes the copy of programs to be passed to the recursive call to safe
				for(int j = 0; j<programs.length; j++){
					for(int k = 0; k<programs[j].length;k++){
						copyPrograms[j][k] = programs[j][k];
					}//end for k
				}//end for j
				
				int[] copyResources = new int[resources.length];//makes the copy of resources to be passed to the recursive call to safe
				for(int j =0; j<resources.length; j++){
					copyResources[j] =resources[j]+copyPrograms[i][j+resources.length];//initalizes copyResources as though job i terminated and returned its resources to the system
					copyPrograms[i][j+resources.length] = 0;//sets all of job i's held resources to 0
				}
				boolean t=safe(copyResources, copyPrograms, numDone+1);//recursive call to safe
				if(t==true)//case where a full correct ordering was found from this point
					return true;
			}//end case where a possible first finisher has been found
		}//end iterating through list of jobs
		return false;//case where no correct ordering was possible with the given program and resource graph 
	}//end safe
	
	public static void fifo(ArrayList<ArrayList<String>>comm, int[] resources){
		ArrayList<ArrayList<String>>commands=new ArrayList<ArrayList<String>>();//full copy of commands list 
		for(int i =0; i<comm.size(); i++){//initialize commands
			commands.add(new ArrayList<String>());
			for(int j=0; j<comm.get(i).size(); j++){
				commands.get(i).add(comm.get(i).get(j));
			}//end for j
		}//end for i
		
		int[]tWaiting = new int[commands.size()];//index i holds how long job i spent waiting
		int[]tTerminated = new int[commands.size()];//index i holds the time at which job i terminated
		int[][]programs=new int[commands.size()][resources.length*2+3];//at programs[i], the vector contains the following information about job i:
		//[the array of its claims][the array of resources it's currently holding][a bit indicating whether the command at commands.get(i).get(0) was executed during the most recent cycle(0 if it was, 1 if it was not)][a bit indicating whether the job has terminated(0 if it has not, 1 if it has)][a bit indicating for how many cycles(if any), the job is stuck computing]
		
		for(int i=0; i<programs.length; i++){//initialize tWaiting and the last three values of each entry of programs
			tWaiting[i]=0;
			programs[i][programs[i].length-3]=0;
			programs[i][programs[i].length-2]=0;
			programs[i][programs[i].length-1]=0;
		}
		
		
		ArrayList<Integer> reqQueue = new ArrayList<Integer>();//fifo queue of unsatisfied requests
		int t=0;
		boolean done = false;
		while(!done){//run simulation until all jobs have terminated
			done = true;			
			
			for(int i =1; i<commands.size(); i++){//checks to see if all jobs have terminated
				if(programs[i][programs[i].length-2] ==0){
					done = false;
				}
			}
			if(!done){//case where not all jobs have terminated
				
				for(int i =1; i<commands.size(); i++){//find any jobs that have have to execute an initiate command next
					String split[] = commands.get(i).get(0).split("\\s+");
					if(split[0].equals("initiate")){
						
						for(int j = 2; j<split.length; j+=2){
							programs[i][Integer.valueOf(split[j])]=Integer.valueOf(split[j+1]);
							programs[i][Integer.valueOf(split[j])+resources.length]=0;
							programs[i][programs[i].length-3]=0;
						}//end initiating the claim for the given resource
					}//end if
				}//end for i
				
				for(int i=1; i<commands.size(); i++){//find programs which have to compute for some number of cycles
					if(programs[i][programs[i].length-1]!=0)//case where program is still stuck computing
						programs[i][programs[i].length-1]--;
				}
				
				for(int i = 1; i<commands.size(); i++){//find jobs whose next command to execute is a compute
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){//case where program is not(/is no longer) stuck computing/has not been terminated
					
						String split[] = commands.get(i).get(0).split("\\s+");//retrieve next instruction the program has to execute
						
						if(split[0].equals("compute")){
							programs[i][programs[i].length-1]=Integer.valueOf(split[2]);//sets number of cycles computation will go for
							commands.get(i).remove(0);
						}//end if
					}//end if
				}//end for i
				
				for(int i = 1; i<commands.size(); i++){//find jobs who have to perform a request next
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){
						String split[] = commands.get(i).get(0).split("\\s+");
						if(split[0].equals("request")){//case where next instruction is a resource request
							boolean toAdd = true;//true if job i is not in the request queue, false otherwise
							
							for(int q=0; q<reqQueue.size();q++){//runs through entire request queue to see ifthere are any matches
								if(reqQueue.get(q)==i)
									toAdd = false;
							}//end for q
							
							if(toAdd){//if job i is not in the request queue, adds it
								reqQueue.add(i);
							}//end if
						}//end if
					}//end if
				}//end for i
				
				for(int q = 0; q<reqQueue.size(); q++){//iterates through request queue, tries to grant requests in FIFO order
					int i=reqQueue.get(q);
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){
						String split[] = commands.get(i).get(0).split("\\s+");
						if(split[0].equals("request")){//case where next instruction is a resource request
						
							int rReq = Integer.valueOf(split[2]);
							int numReq = Integer.valueOf(split[3]);
							
							if(numReq<=resources[rReq]&&!(numReq+programs[i][rReq+resources.length]>programs[i][rReq])){//case where system has enough to grant request and request does not violate the job's claims
								programs[i][rReq+resources.length]+=numReq;//gives the job the desired amount of the desired resource
								resources[rReq]-=numReq;//removes this amount of this resource from the system
								programs[i][programs[i].length-3]=0;//indicates the request was satisfied in this cycle
							}//end if 							
						
							else if(numReq+programs[i][rReq+resources.length]>programs[i][rReq]){//case where request violates the job i's claims
								for(int res =0; res<resources.length; res++){//returns job i's resources to the system
									resources[res]+=programs[i][res+resources.length];
									programs[i][res+resources.length]=0;
								}//end for res
								
								programs[i][programs[i].length-2]=1;//job i terminated
								tTerminated[i] = -1;//job i aborted
							}//end if
							else if (!(numReq<=resources[rReq])){//case where system doesn't have enough to satisfy the request
								tWaiting[i]++;
								programs[i][programs[i].length-3]=1;
							}//end if
						
						}//end resource request case
					}//end if
				}//end for q
				
				for(int i = 1; i<commands.size(); i++){//find jobs which have to execute a release next
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){
						String split[] = commands.get(i).get(0).split("\\s+");
						 if(split[0].equals("release")){
							int rRel = Integer.valueOf(split[2]);
							int numRel = Integer.valueOf(split[3]);
						
							resources[rRel]+=numRel;//add resource back to the system
							programs[i][rRel+resources.length]-=numRel;//take resource back from program
							programs[i][programs[i].length-3]=0;//indicate command was executed
						}//end if
					}//end if
				}//end for i
				
				for(int i = 1; i<commands.size(); i++){//find jobs which have to execute a terminate command next
					if(programs[i][programs[i].length-1]==0&&programs[i][programs[i].length-2]==0){
						String split[] = commands.get(i).get(0).split("\\s+");
						if(split[0].equals("terminate")){
							for(int j =1; j<resources.length; j++)//return job i's resources to the system
								resources[j]+=programs[i][j+resources.length];
							
								programs[i][programs[i].length-2]=1;//set job to terminated
								tTerminated[i]=t;
						}//end if
					}//end if
				}//end for i
				
				for(int i = 1; i<commands.size(); i++){
					if(programs[i][programs[i].length-3]==0&&programs[i][programs[i].length-2]==0&&programs[i][programs[i].length-1]==0){
						commands.get(i).remove(0);
						for(int q=0; q<reqQueue.size(); q++){
							if(reqQueue.get(q)==i)
								reqQueue.remove((int)q);
						}
					}
				}

				boolean deadlocked = true;//looks for deadlock
				boolean terminated = true;
				for(int i = 1; i<commands.size(); i++){
					if(programs[i][programs[i].length-2]!=1)
						terminated = false;
					if(programs[i][programs[i].length-3]==0&&programs[i][programs[i].length-2]!=1)
						deadlocked = false;
				}
				
				boolean found;
				while(deadlocked&&!terminated){
					found=false;//false if a program has not yet been aborted to remove deadlock, true otherwise
					for(int i = 1; i<commands.size(); i++){//aborts the first non-terminated program
						if(programs[i][programs[i].length-2]!=1&&!found){
							found=true;
							for(int res =0; res<resources.length; res++){
								resources[res]+=programs[i][res+resources.length];
								programs[i][res+resources.length]=0;
							}
							programs[i][programs[i].length-2]=1;
							tTerminated[i] = -1;
						}
					}//end for
					
					for(int i=1; i<commands.size(); i++){//checks if the program that was just aborted was enough to remove the deadlock
						if(programs[i][programs[i].length-2]!=1){
							String[] split = commands.get(i).get(0).split("\\s+");
							int rReq = Integer.valueOf(split[2]);
							int numReq = Integer.valueOf(split[3]);
							if(numReq<=resources[rReq])//case where system is no longer deadlocked
								deadlocked = false;
						}
					}//end checking if system is still deadlocked
				}//end while
					
			}//end if(!done)
		t++;
		}//end while(!done)
		
		System.out.println("FIFO");
		int totalComputation = 0;
		int totalWaiting = 0;
		
		for(int i = 1; i<tTerminated.length; i++){
			if(tTerminated[i]!=-1){
				totalComputation+=tTerminated[i];
				totalWaiting+=tWaiting[i];
				System.out.println("Task "+i+"\t\t"+tTerminated[i]+"\t"+tWaiting[i]+"\t"+(int)((((double)tWaiting[i])/((double)tTerminated[i]))*100)+"%");
			}
			else
				System.out.println("Task "+i+"\t\taborted");
				
		}
		System.out.println("total\t\t"+totalComputation+"\t"+totalWaiting+"\t"+(int)((((double)totalWaiting)/((double)totalComputation))*100)+"%");
	}//end FIFO
}//end class

