/*
 *  Copyright 2011 RoboViz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package rv.world;

import java.util.ArrayList;
import java.util.List;

import js.jogl.model.ObjMaterial;
import rv.comm.rcssserver.GameState;
import rv.comm.rcssserver.ISceneGraphItem;
import rv.comm.rcssserver.GameState.GameStateChangeListener;
import rv.comm.rcssserver.ServerComm;
import rv.comm.rcssserver.ServerComm.ServerChangeListener;
import rv.comm.rcssserver.scenegraph.Node;
import rv.comm.rcssserver.scenegraph.SceneGraph;
import rv.comm.rcssserver.scenegraph.StaticMeshNode;
import rv.content.ContentManager;
import rv.world.objects.Agent;

/**
 * A team of RoboCup agents
 * 
 * @author Justin Stoecker
 */
public class Team implements ISceneGraphItem, GameStateChangeListener {
    public static final int LEFT  = 0;
    public static final int RIGHT = 1;

    private static final int MAX_AGENTS = 11;
    
    private ContentManager  content;
    private final int       id;
    private String          name;
    private List<Agent>     agents;
    private ObjMaterial     teamColor;
    private int             score;

    public ObjMaterial getTeamMaterial() {
        return teamColor;
    }

    public int getScore() {
        return score;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the team's ID number where left is Team.LEFT and right is
     * Team.RIGHT
     */
    public int getID() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Clears all agents on the team.
     */
    public void resetPlayers() {
        agents.clear();
    }

    public Team(float[] color, int id, ContentManager content) {
        this.id = id;
        this.content = content;
        agents = new ArrayList<Agent>();
        
        name = (id == LEFT) ? "<left>" : "<right>";

        teamColor = new ObjMaterial(name);
        teamColor.setDiffuse(color);
        teamColor.setAmbient(color);
        teamColor.setShininess(96);
        teamColor.setSpecular(1, 1, 1, 1);
    }

    @Override
    public void sceneGraphChanged(SceneGraph sg) {
        agents.clear();

        // Add agents from scene graph to this team
        // TODO: hopefully we can have the scene graph store agent IDs directly
        // to avoid this computation and guessing
        for (int i = 1; i <= MAX_AGENTS; i++) {
            Node agentNode = findAgent(i, sg);
            if (agentNode != null)
                agents.add(new Agent(this, i, agentNode, sg, content));
        }
        
//        do {
//            int agentID = agents.size() + 1;
//            agentNode = findAgent(agentID, sg);
//            if (agentNode != null)
//                agents.add(new Agent(this, agentID, agentNode, sg, content));
//        } while (agentNode != null);
    }

    @Override
    public void update(SceneGraph sg) {
        for (int i = 0; i < agents.size(); i++)
            agents.get(i).update(sg);
    }

    public Agent getAgentByID(int id) {
        for (Agent a : agents)
            if (a.getID() == id)
                return a;
        return null;
    }
    
    /**
     * Check scene graph to see if an agent on this team with given agentID can
     * be located. Returns the root node containing that agent's mesh nodes if
     * it can be found; otherwise, returns null.
     */
    private Node findAgent(int agentID, SceneGraph sg) {
        // the agents might be added in any order (two teams joining at once),
        // so the "root" node is found by finding the branch containing a mesh
        // with specific materials that only belong to this agent
        String matAgentID = "matNum" + agentID;
        String matTeamID = id == Team.LEFT ? "matLeft" : "matRight";
        String[] materials = { matAgentID, matTeamID };

        // see if a node can be found with these materials
        StaticMeshNode leaf = sg.findStaticMeshNode(sg.getRoot(), materials);
        if (leaf == null)
            return null;

        // root of the agent node is one level lower than root of entire scene
        // graph
        Node root = leaf;
        if (root.getParent() == null)
            return root;
        while (root.getParent().getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    @Override
    public void gsMeasuresAndRulesChanged(GameState gs) {
    }

    @Override
    public void gsPlayStateChanged(GameState gs) {
        // update team name & score
        if (id == LEFT) {
            name = gs.getTeamLeft() == null ? "<left>" : gs.getTeamLeft();
            score = gs.getScoreLeft();
        } else {
            name = gs.getTeamRight() == null ? "<right>" : gs.getTeamRight();
            score = gs.getScoreRight();
        }
    }

    @Override
    public void gsTimeChanged(GameState gs) {
    }
}
