
import com.cometway.ak.Agent;
import com.cometway.om.IObjectManager;
import com.cometway.om.ObjectManager;
import com.cometway.om.PropsType;
import com.cometway.om.ObjectID;
import com.cometway.props.Props;
import com.cometway.util.*;
import java.util.*;

/**
* This agent excercises the resident object manager for the specified number of iterations.
* This code can be used as an example of interacting with the object manager, as well as
* a decent tool for testing any object manager.
* For each iteration the following operations occur:
* 1) an object of PropsType "test" is created
* 2) properties for that object are set
* 3) objects of PropsType "test" are listed
* 4) the previously created object is retrieved by ID
* 5) the previously created object is removed
*/

public class ObjectManagerTest extends Agent
{
	public void initProps()
	{
		setDefault("iterations", "100");
	}


	public void start()
	{
		println("Getting a reference to the object manager");

		com.cometway.om.IObjectManager   om = ObjectManager.getObjectManager();
		int				iterations = getInteger("iterations");

		for (int i = 0; i < iterations; i++)
		{
			PropsType       testType = new PropsType("test");
			ObjectID	id = om.createObject(testType);

			println("created " + id);

			Props   p = (Props) om.getObject(id);

			p.setBoolean("tagged", true);
			p.setProperty("check", "123456");

			Vector		v = om.listObjects(testType);
			Enumeration     e = v.elements();

			while (e.hasMoreElements())
			{
				println(e.nextElement().toString());
			}

			p = (Props) om.getObject(id);

			p.dump();
			om.removeObject(id);
		}
	}


}

