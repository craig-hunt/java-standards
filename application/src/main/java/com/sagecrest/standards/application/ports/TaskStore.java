package com.sagecrest.standards.application.ports;

import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import java.util.List;

/**
 * The persistence this feature calls, and nothing more.
 *
 * <p>The interface lives beside its caller rather than beside its implementation, so infrastructure
 * depends on application and never the reverse. A store that grows a method no feature calls has
 * grown it for the wrong reason.
 *
 * <p>Every method blocks. The C# sibling returns a {@code Task} and threads a {@code
 * CancellationToken} through every signature, because a .NET request thread is too expensive to
 * park on a database. A Java 21 request runs on a virtual thread, where parking costs a heap
 * object, so blocking here is both cheaper and plainer. The asynchrony has not been hidden: it has
 * been moved to the scheduler, which is the only component that ever needed to know about it.
 */
public interface TaskStore {

  List<TaskItem> list();

  TaskItem create(TaskTitle title);

  TaskItem setCompleted(TaskId id, boolean completed);

  void delete(TaskId id);

  long deleteCompleted();
}
