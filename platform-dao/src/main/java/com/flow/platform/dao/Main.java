package com.flow.platform.dao;

import com.flow.platform.domain.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.sql.Time;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Will on 17/6/7.
 */
public class Main {
    private static SessionFactory factory;
    public static void main(String[] args) {
        try{
            factory = new Configuration().
                    configure("hibernate.cfg.xml").
                    //addPackage("com.xyz") //add package if used.
//                            addAnnotatedClass(AgentModel.class).
//                            addAnnotatedClass(CmdModel.class).
        buildSessionFactory();
        }catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }

//
//        AgentDaoImp adi = new AgentDaoImp();
//        adi.setSessionFactory(factory);
//        AgentModel am = adi.create(new Agent("XXX", "sss"));
//        am.setAgentPath("ABC");
//        adi.update(am);
//
//        List<AgentModel> list =  adi.list("AAAAAAA", "OFFLINE");
//
//        CmdBase cb = new CmdBase("AAAAAAA", "ABC", CmdBase.Type.KILL, "RUN_SHELL");
//        Cmd cmd = new Cmd(cb);
//        CmdDaoImp cmdDaoImp = new CmdDaoImp();
//        cmdDaoImp.setSessionFactory(factory);
//        cmdDaoImp.create(cmd);




//        Session session = factory.openSession();
//        session.beginTransaction();
//        Agent agent = new Agent("XXX", "sss");
//        agent.setSessionDate(new Date());
//        agent.setStatus(AgentStatus.BUSY);
//        session.save( agent);
//        session.getTransaction().commit();

          DaoBase daobase = new DaoBase();
        daobase.setSessionFactory(factory);
        Agent agent = new Agent("XXX", "sss");
        Agent agent1 = daobase.save(agent);

        agent.setStatus(AgentStatus.IDLE);
        agent = daobase.update(agent);

        AgentPath agentPath = new AgentPath("test1", "jinan");
        Cmd cmd = new Cmd(new CmdBase(agentPath, CmdType.KILL, "ls"));
        cmd.setId("000000000000001");
        cmd.setAgent(agent);

        CmdResult cmdResult = new CmdResult();
        cmdResult.setCmd(cmd);
        cmd.setResult(cmdResult);
        daobase.save(cmd);
        daobase.save(cmdResult);

//        daobase.delete(agent);
//
//        agent = daobase.get(Agent.class, agent.getId());



//
//        AgentPath agentPath = new AgentPath("test1", "jinan");
//        Cmd cmd = new Cmd(new CmdBase(agentPath, CmdType.KILL, "ls"));
//        session.save(cmd);

//        AgentModel am = new AgentModel(new Agent("XXX", "sss"));

//      /* Add few employee records in database */
//        Integer empID1 = ME.addEmployee("Zara", "Ali", 1000);
//        Integer empID2 = ME.addEmployee("Daisy", "Das", 5000);
//        Integer empID3 = ME.addEmployee("John", "Paul", 10000);
//
//      /* List down all the employees */
//        ME.listEmployees();
//
//      /* Update employee's records */
//        ME.updateEmployee(empID1, 5000);
//
//      /* Delete an employee from the database */
//        ME.deleteEmployee(empID2);
//
//      /* List down new list of the employees */
//        ME.listEmployees();


//        Cmd cmd2 = daobase.get(Cmd.class, cmd.getId());
//        CmdResult cmdResult3 = cmd2.getResult();

        CmdResult cmdResult1 = daobase.get(CmdResult.class, cmdResult.getId());
        Cmd cmd1 = cmdResult1.getCmd();
        cmd1.setStatus(CmdStatus.EXECUTED);
        daobase.update(cmd1);
        CmdResult cmdResult2 = cmd1.getResult();
        cmdResult2.setStartTime(new Date());
        cmdResult2.setFinishTime(new Date());
        daobase.update(cmdResult2);

    }
//    /* Method to CREATE an employee in the database */
//    public Integer addEmployee(String fname, String lname, int salary){
//        Session session = factory.openSession();
//        Transaction tx = null;
//        Integer employeeID = null;
//        try{
//            tx = session.beginTransaction();
//            Employee employee = new Employee();
//            employee.setFirstName(fname);
//            employee.setLastName(lname);
//            employee.setSalary(salary);
//            employeeID = (Integer) session.save(employee);
//            tx.commit();
//        }catch (HibernateException e) {
//            if (tx!=null) tx.rollback();
//            e.printStackTrace();
//        }finally {
//            session.close();
//        }
//        return employeeID;
//    }
//    /* Method to  READ all the employees */
//    public void listEmployees( ){
//        Session session = factory.openSession();
//        Transaction tx = null;
//        try{
//            tx = session.beginTransaction();
//            List employees = session.createQuery("FROM Employee").list();
//            for (Iterator iterator =
//                 employees.iterator(); iterator.hasNext();){
//                Employee employee = (Employee) iterator.next();
//                System.out.print("First Name: " + employee.getFirstName());
//                System.out.print("  Last Name: " + employee.getLastName());
//                System.out.println("  Salary: " + employee.getSalary());
//            }
//            tx.commit();
//        }catch (HibernateException e) {
//            if (tx!=null) tx.rollback();
//            e.printStackTrace();
//        }finally {
//            session.close();
//        }
//    }
//    /* Method to UPDATE salary for an employee */
//    public void updateEmployee(Integer EmployeeID, int salary ){
//        Session session = factory.openSession();
//        Transaction tx = null;
//        try{
//            tx = session.beginTransaction();
//            Employee employee =
//                    (Employee)session.get(Employee.class, EmployeeID);
//            employee.setSalary( salary );
//            session.update(employee);
//            tx.commit();
//        }catch (HibernateException e) {
//            if (tx!=null) tx.rollback();
//            e.printStackTrace();
//        }finally {
//            session.close();
//        }
//    }
//    /* Method to DELETE an employee from the records */
//    public void deleteEmployee(Integer EmployeeID){
//        Session session = factory.openSession();
//        Transaction tx = null;
//        try{
//            tx = session.beginTransaction();
//            Employee employee =
//                    (Employee)session.get(Employee.class, EmployeeID);
//            session.delete(employee);
//            tx.commit();
//        }catch (HibernateException e) {
//            if (tx!=null) tx.rollback();
//            e.printStackTrace();
//        }finally {
//            session.close();
//        }
//    }
}
