package com.frank.service;

import com.frank.dto.CountryInfoResponse;
import com.frank.dto.CountryInfoUpdateRequest;
import com.frank.entity.CountryInfo;
import com.frank.entity.Language;
import com.frank.exception.CountryNotFoundException;
import com.frank.exception.DuplicateCountryException;
import com.frank.repository.CountryInfoRepository;
import com.frank.soap.CountryInfoSoapClient;
import com.frank.soap.FullCountryInfoData;
import com.frank.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CountryInfoService {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoService.class);

    private final CountryInfoRepository countryInfoRepository;
    private final CountryInfoSoapClient soapClient;

    public CountryInfoService(CountryInfoRepository countryInfoRepository,
                              CountryInfoSoapClient soapClient) {
        this.countryInfoRepository = countryInfoRepository;
        this.soapClient = soapClient;
    }

    @Transactional
    public CountryInfoResponse importCountryByName(String rawCountryName) {
        String countryName = TextUtils.toSentenceCase(rawCountryName);
        log.info("Importing country by name raw={} normalized={}", rawCountryName, countryName);

        String isoCode = soapClient.fetchCountryIsoCode(countryName);
        if (countryInfoRepository.existsByIsoCode(isoCode)) {
            throw new DuplicateCountryException(isoCode);
        }

        FullCountryInfoData soapData = soapClient.fetchFullCountryInfo(isoCode);
        CountryInfo saved = countryInfoRepository.save(mapToEntity(soapData));
        log.info("Country imported successfully id={} isoCode={}", saved.getId(), saved.getIsoCode());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "countries", key = "'all'")
    public List<CountryInfoResponse> findAll() {
        log.info("Fetching all countries");
        return countryInfoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "countries", key = "#id")
    public CountryInfoResponse findById(Long id) {
        log.info("Fetching country by id={}", id);
        CountryInfo countryInfo = countryInfoRepository.findById(id)
                .orElseThrow(() -> new CountryNotFoundException(id));
        return toResponse(countryInfo);
    }

    @Transactional
    @CacheEvict(value = "countries", allEntries = true)
    public CountryInfoResponse update(Long id, CountryInfoUpdateRequest request) {
        log.info("Updating country id={}", id);
        CountryInfo countryInfo = countryInfoRepository.findById(id)
                .orElseThrow(() -> new CountryNotFoundException(id));

        countryInfo.setName(TextUtils.toSentenceCase(request.name()));
        countryInfo.setCapitalCity(request.capitalCity());
        countryInfo.setPhoneCode(request.phoneCode());
        countryInfo.setContinentCode(request.continentCode());
        countryInfo.setCurrencyCode(request.currencyCode());
        countryInfo.setCountryFlagUrl(request.countryFlagUrl());
        countryInfo.clearLanguages();

        if (request.languages() != null) {
            request.languages().forEach(languageName -> {
                Language language = new Language();
                language.setName(languageName);
                countryInfo.addLanguage(language);
            });
        }

        return toResponse(countryInfoRepository.save(countryInfo));
    }

    @Transactional
    @CacheEvict(value = "countries", allEntries = true)
    public void delete(Long id) {
        log.info("Deleting country id={}", id);
        if (!countryInfoRepository.existsById(id)) {
            throw new CountryNotFoundException(id);
        }
        countryInfoRepository.deleteById(id);
    }

    private CountryInfo mapToEntity(FullCountryInfoData data) {
        CountryInfo countryInfo = new CountryInfo();
        countryInfo.setIsoCode(data.isoCode());
        countryInfo.setName(data.name());
        countryInfo.setCapitalCity(data.capitalCity());
        countryInfo.setPhoneCode(data.phoneCode());
        countryInfo.setContinentCode(data.continentCode());
        countryInfo.setCurrencyCode(data.currencyCode());
        countryInfo.setCountryFlagUrl(data.countryFlagUrl());

        if (data.languages() != null) {
            data.languages().forEach(languageName -> {
                Language language = new Language();
                language.setName(languageName);
                countryInfo.addLanguage(language);
            });
        }

        return countryInfo;
    }

    private CountryInfoResponse toResponse(CountryInfo countryInfo) {
        List<String> languages = countryInfo.getLanguages().stream()
                .map(Language::getName)
                .toList();

        return new CountryInfoResponse(
                countryInfo.getId(),
                countryInfo.getIsoCode(),
                countryInfo.getName(),
                countryInfo.getCapitalCity(),
                countryInfo.getPhoneCode(),
                countryInfo.getContinentCode(),
                countryInfo.getCurrencyCode(),
                countryInfo.getCountryFlagUrl(),
                languages
        );
    }
}
