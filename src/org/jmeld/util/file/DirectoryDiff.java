/*
   JMeld is a visual diff and merge tool.
   Copyright (C) 2007  Kees Kuip
   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.
   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.
   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor,
   Boston, MA  02110-1301  USA
 */
package org.jmeld.util.file;

import org.apache.jmeld.tools.ant.*;
import org.jmeld.settings.*;
import org.jmeld.settings.util.*;
import org.jmeld.ui.*;
import org.jmeld.util.*;
import org.jmeld.util.node.*;

import java.io.*;
import java.util.*;

public class DirectoryDiff
       extends FolderDiff
{
  private File                    rightDirectory;
  private File                    leftDirectory;
  private JMDiffNode              rootNode;
  private Map<String, JMDiffNode> nodes;
  private Filter                  filter;

  public DirectoryDiff(
    File   leftDirectory,
    File   rightDirectory,
    Filter filter)
  {
    this.leftDirectory = leftDirectory;
    this.rightDirectory = rightDirectory;
    this.filter = filter;

    try
    {
      setLeftFolderShortName(leftDirectory.getName());
      setRightFolderShortName(rightDirectory.getName());
      setLeftFolderName(leftDirectory.getCanonicalPath());
      setRightFolderName(rightDirectory.getCanonicalPath());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  public JMDiffNode getRootNode()
  {
    return rootNode;
  }

  public Collection<JMDiffNode> getNodes()
  {
    return nodes.values();
  }

  public void diff(Mode mode)
  {
    DirectoryScanner ds;
    JMDiffNode       node;

    StatusBar.start();
    StatusBar.setState("Start scanning directories...");

    rootNode = new JMDiffNode("<root>", false);
    nodes = new HashMap<String, JMDiffNode>();

    ds = new DirectoryScanner();
    ds.setShowStateOn(true);
    ds.setBasedir(leftDirectory);
    if (filter != null)
    {
      ds.setIncludes(filter.getIncludes());
      ds.setExcludes(filter.getExcludes());
    }
    ds.setCaseSensitive(true);
    ds.scan();

    for (FileNode fileNode : ds.getIncludedFilesMap().values())
    {
      node = addNode(fileNode.getName());
      node.setBufferNodeLeft(fileNode);
    }

    ds = new DirectoryScanner();
    ds.setShowStateOn(true);
    ds.setBasedir(rightDirectory);
    if (filter != null)
    {
      ds.setIncludes(filter.getIncludes());
      ds.setExcludes(filter.getExcludes());
    }
    ds.setCaseSensitive(true);
    ds.scan();

    for (FileNode fileNode : ds.getIncludedFilesMap().values())
    {
      node = addNode(fileNode.getName());
      node.setBufferNodeRight(fileNode);
    }

//System.out.println("start comparing " + nodes.size() + " nodes");
    for (JMDiffNode n : nodes.values())
    {
      n.compareContents();
    }
System.out.println("end comparing " + nodes.size() + " nodes");

    StatusBar.setState("Ready comparing directories");
    StatusBar.stop();
  }

  private JMDiffNode addNode(String name)
  {
    JMDiffNode node;

    node = nodes.get(name);
    if (node == null)
    {
      node = addNode(new JMDiffNode(name, true));
    }

    return node;
  }

  private JMDiffNode addNode(JMDiffNode node)
  {
    String     parentName;
    JMDiffNode parent;

    nodes.put(
      node.getName(),
      node);

    parentName = node.getParentName();
    if (StringUtil.isEmpty(parentName))
    {
      parent = rootNode;
    }
    else
    {
      parent = nodes.get(parentName);
      if (parent == null)
      {
        parent = addNode(new JMDiffNode(parentName, false));
      }
    }

    parent.addChild(node);
    return node;
  }

  public void print()
  {
    rootNode.print("");
  }

  public static void main(String[] args)
  {
    DirectoryDiff diff;
    StopWatch     stopWatch;

    diff = new DirectoryDiff(
        new File(args[0]),
        new File(args[1]),
        JMeldSettings.getInstance().getFilter().getFilter("default"));
    stopWatch = new StopWatch();
    stopWatch.start();
    diff.diff(DirectoryDiff.Mode.TWO_WAY);
    System.out.println("diff took " + stopWatch.getElapsedTime() + " msec.");
    diff.print();
  }
}
