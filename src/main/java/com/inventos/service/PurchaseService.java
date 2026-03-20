package com.inventos.service;

import com.inventos.dto.request.PurchaseRequest;
import com.inventos.dto.response.PurchaseResponse;
import java.util.List;

public interface PurchaseService {
    List<PurchaseResponse> getAll();
    PurchaseResponse getById(Long id);
    PurchaseResponse create(PurchaseRequest req, String username);
    PurchaseResponse update(Long id, PurchaseRequest req);
    void delete(Long id);
}
