package com.interviewprep.service;

import com.interviewprep.dto.TaskRequest;
import com.interviewprep.dto.TaskResponse;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.model.Task;
import com.interviewprep.model.TaskStatus;
import com.interviewprep.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(request.title(), request.description(), statusOrDefault(request), request.dueDate());
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null ? repository.findAll() : repository.findByStatus(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = find(id);
        task.update(request.title(), request.description(), statusOrDefault(request), request.dueDate());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Task find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private static TaskStatus statusOrDefault(TaskRequest request) {
        return request.status() == null ? TaskStatus.TODO : request.status();
    }
}
