package kopo.poly.service.impl;

import kopo.poly.dto.MovieDTO;
import kopo.poly.mapper.IMovieMapper;
import kopo.poly.service.IMovieService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MovieService implements IMovieService {

    // RequiredArgsConstructor 어노테이션으로 생성자를 자동 생성함
    // movieMapper 변수에 이미 메모리에 올라간 Mapper 객체를 넣어줌
    // 예전에는 autowired 어노테이션을 통해 설정했었지만, 이젠 생성자를 통해 객체 주입함
    private final IMovieMapper movieMapper;

    /**
     * JSOUP 라이브러리를 통한 영화 순위 정보 가져오기
     */
    @Transactional
    @Override
    public int collectMovieRank() throws Exception {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.collectMovieRank Start!", this.getClass().getName());

        String collectTime = DateUtil.getDateTime("yyyy-MM-dd"); // 수집날짜 = 오늘 날짜

        MovieDTO pDTO = new MovieDTO();
        pDTO.setCollectTime(collectTime);

        // 기존에 수집된 영화 순위 데이터 삭제하기
        movieMapper.deleteMovieInfo(pDTO);

        pDTO = null; // 기존 등록된 영화 순위 삭제 후, pDTO 값 제거하기

        int res = 0; // 크롤링 결과 (0보다 크면 크롤링 성공)

        // 영화 순위 정보 가져올 사이트 주소
        String url = "https://www.kobis.or.kr/kobis/business/stat/boxs/findRealTicketList.do?loadEnd=0";

        // JSOUP 라이브러리를 통해 사이트 접속되면, 그 사이트의 전체 HTML 소스 저장할 변수
        Document doc;

        // 사이트 접속(http프로토콜만 가능, https 프로토콜은 보안상 안 됨)
        doc = Jsoup.connect(url).get();

        // CGV 웹페이지의 전체 소스 중 일부 태그를 선택하기 위해 사용
        // <div class="sect-movie=chart"> 이 태그 내에서 있는 HTML소스만 element에 저장됨
        Elements element = doc.select("tbody");

        // Iterator을 사용하여 영화 순위 정보를 가져오기
        // 영화순위는 기본적으로 1개 이상의 영화가 존재하기 때문에 태그의 반복이 존재할 수 밖에 없음
        Iterator<Element> movie_rank = element.select("td:nth-child(1)").iterator(); // 영화 순위
        Iterator<Element> movie_name = element.select("td.tal").iterator(); // 영화 이름
        Iterator<Element> movie_reserve = element.select("td:nth-child(4)").iterator(); // 영화 예매율
//        Iterator<Element> score = element.select("span.percent").iterator(); // 점수
        Iterator<Element> open_day = element.select("td:nth-child(3)").iterator(); // 개봉일

        // 수집된 데이터 DB에 저장하기
        while (movie_rank.hasNext()) {

            pDTO = new MovieDTO(); // 수집된 영화정보를 DTO에 저장하기 위해 메모리에 올리기

            // 수집시간을 기본키(pk)로 사용
            pDTO.setCollectTime(collectTime);

            // 영화 순위(trim 함수 추가 이유 : trim 함수는 글자의 앞 뒤 공백을 삭제하는 역할을 수행하며, 데이터 수집 시
            // 홈페이지 개발자들을 앞 뒤 공백을 집어넣을 수 있어서 추가
            String rank = CmmUtil.nvl(movie_rank.next().text()).trim(); // No.1 들어옴
            pDTO.setMovieRank(rank/* .substring(3, rank.length()) */);

            // 영화 제목
            pDTO.setMovieNm(CmmUtil.nvl(movie_name.next().text()).trim());

            // 영화 예매율
            pDTO.setMovieReserve(CmmUtil.nvl(movie_reserve.next().text()).trim());

            // 영화 점수
//            pDTO.setScore(CmmUtil.nvl(score.next().text()).trim());


            pDTO.setOpenDay(CmmUtil.nvl(open_day.next().text(), "----------").trim().substring(0, 10));

            // 등록자
            pDTO.setRegId("admin");

            // 영화 한개씩 추가
            res += movieMapper.insertMovieInfo(pDTO);

        }

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.collectMovieRank End!", this.getClass().getName());

        return res;
    }

    @Override
    public List<MovieDTO> getMovieInfo() throws Exception {

        log.info("{}.collectMovieRank Start!", this.getClass().getName());

        MovieDTO pDTO = new MovieDTO();

        pDTO.setCollectTime(DateUtil.getDateTime("yyyy-MM-dd"));

        log.info("{}.collectMovieRank End!", this.getClass().getName());

        return movieMapper.getMovieInfo(pDTO);
    }
}
